package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.JDK;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

/**
 * 
 * @author Tim Bacon
 */
public class FitnesseExecutor {
	private static final int SLEEP_MILLIS = 1000;
	private static final int STARTUP_TIMEOUT_MILLIS = 30 * 1000;
	private static final int READ_PAGE_TIMEOUT = 10 * 1000;

	private final FitnesseBuilder builder;
	private final EnvVars envVars;
	private final PrintStream logger;
	private final BuildListener listener;

	public FitnesseExecutor(FitnesseBuilder builder, BuildListener listener, EnvVars envVars) {
		this.builder = builder;
		this.listener = listener;
		this.envVars = envVars;
		this.logger = listener.getLogger();
	}

	public boolean execute(Launcher launcher, AbstractBuild<?, ?> build) throws InterruptedException {
		Proc fitnesseProc = null;
		try {
			build.addAction(getFitnesseBuildAction(build));
			FilePath workingDirectory = getWorkingDirectory(logger, build);
			if (builder.getFitnesseStart()) {
				fitnesseProc = startFitnesse(workingDirectory, launcher);
				if (!fitnesseProc.isAlive() || !isFitnesseStarted(getFitnessePage(build, false))) {
					return false;
				}
			}

			FilePath resultsFilePath = getFilePath(logger, workingDirectory, builder.getFitnessePathToXmlResultsOut(envVars));
			readAndWriteFitnesseResults(getFitnessePage(build, true), resultsFilePath);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(logger);
			if (t instanceof InterruptedException)
				throw (InterruptedException) t;
			return false;
		} finally {
			killProc(fitnesseProc);
		}
	}

	private FitnesseBuildAction getFitnesseBuildAction(AbstractBuild<?, ?> build) throws IOException {
		return new FitnesseBuildAction(builder.getFitnesseStart(), builder.getFitnesseHost(build, envVars),
				builder.getFitnessePort());
	}

	private Proc startFitnesse(FilePath workingDirectory, Launcher launcher) throws IOException, InterruptedException {
		logger.println("Starting new Fitnesse instance...");
		ProcStarter procStarter = launcher.launch().cmds(getJavaCmd(workingDirectory));
		procStarter.pwd(getFilePath(workingDirectory, builder.getFitnesseJavaWorkingDirectory()));
		procStarter.stdout(logger).stderr(logger);
		return procStarter.start();
	}

	public ArrayList<String> getJavaCmd(FilePath workingDirectory) throws IOException, InterruptedException {
		String java = null;

		// master/salve configuration
		if (!builder.getFitnesseJdk(envVars).isEmpty()) {
			JDK jdk = Jenkins.getInstance().getJDK(builder.getFitnesseJdk(envVars));
			if (jdk != null) {
				jdk = jdk.forNode(Computer.currentComputer().getNode(), listener);
				java = getJavaBinFromjavaHome(workingDirectory, jdk.getHome());
			}
		}
		// env variable
		if (java == null && envVars.containsKey("JAVA_HOME")) {
			java = getJavaBinFromjavaHome(workingDirectory, envVars.get("JAVA_HOME"));
		}
		// default: use java declared in path
		if (java == null) {
			java = "java";
		}

		String fitnesseJavaOpts = builder.getFitnesseJavaOpts(envVars);
		String[] java_opts = ("".equals(fitnesseJavaOpts) ? new String[0] : fitnesseJavaOpts.split(" "));

		String absolutePathToFitnesseJar = getAbsolutePathToFile(workingDirectory, builder.getFitnessePathToJar());
		String[] jar_opts = { "-jar", absolutePathToFitnesseJar };

		FilePath absolutePathToFitNesseRoot = getFilePath(workingDirectory, builder.getFitnessePathToRoot());
		String[] fitnesse_opts = { // --
		"-d", absolutePathToFitNesseRoot.getParent().getRemote(), // --
				"-r", absolutePathToFitNesseRoot.getName(), // --
				"-p", Integer.toString(builder.getFitnessePort()) };

		// split additional fitness options and add them to those explicitly configured ones
		String[] addOps = splitOptions(builder.getAdditionalFitnesseOptions());

		String[] fitnesse_opts2 = new String[fitnesse_opts.length + addOps.length];
		System.arraycopy(fitnesse_opts, 0, fitnesse_opts2, 0, fitnesse_opts.length);
		System.arraycopy(addOps, 0, fitnesse_opts2, fitnesse_opts.length, addOps.length);

		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(java);
		if (java_opts.length > 0)
			cmd.addAll(Arrays.asList(java_opts));
		cmd.addAll(Arrays.asList(jar_opts));
		cmd.addAll(Arrays.asList(fitnesse_opts2));

		return cmd;
	}

	private String getJavaBinFromjavaHome(FilePath workingDirectory, String javaHome) throws IOException,
			InterruptedException {
		FilePath javaHomePath = getFilePath(workingDirectory, javaHome);
		if (javaHomePath.exists()) {
			return javaHomePath.child("bin").child("java").getRemote();
		}
		return null;
	}

	/**
	 * Breaks the given string down by any options of the form "-x" or
	 * "-x some argument not containing a - character"
	 */
	private static String[] splitOptions(String string) {
		List<String> addOps = new ArrayList<String>();

		// match pattern to identify additional cmd arguments
		Pattern pattern = Pattern.compile("-{1}[a-z]{1}\\s?[^-]*");
		Matcher m = pattern.matcher(string);

		while (m.find()) {
			String s = m.group();
			addOps.add(s.substring(0, 2).trim());
			addOps.add(s.substring(2, s.length()).trim());
		}
		String[] ret = new String[addOps.size()];
		return addOps.toArray(ret);
	}

	/**
	 * Detect if fitnesse has started by try to do an HTTP connection
	 * 
	 * @return true if fitnesse has started, false otherwise
	 */
	public boolean isFitnesseStarted(URL fitnessePageURL) throws InterruptedException {
		long waitedAlready;
		boolean launched = false;
		logger.println("Wait for Fitnesse Server start");
		for (waitedAlready = 0; waitedAlready < STARTUP_TIMEOUT_MILLIS; waitedAlready += SLEEP_MILLIS) {
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) fitnessePageURL.openConnection();
				connection.setRequestMethod("GET"); // HEAD is not allowed on Fitnesse
																						// server (error 400)
				connection.setReadTimeout(READ_PAGE_TIMEOUT);
				int responseCode = connection.getResponseCode();
				if (responseCode != 200)
					throw new RuntimeException(String.format("Response for page %s is %d", fitnessePageURL, responseCode));
				launched = true;
				break;
			} catch (IOException e) {
				logger.print('.');
				logger.flush();
				Thread.sleep(SLEEP_MILLIS);
				launched = false;
			} finally {
				if (connection != null)
					connection.disconnect();
			}
		}

		logger.printf(launched // --
		? "%nFitnesse server started in %sms.%n" // --
				: "%nFitnesse server NOT started in %sms on URL: %s%n", waitedAlready, fitnessePageURL);

		return launched;
	}

	private void killProc(Proc proc) {
		if (proc != null) {
			try {
				proc.kill();
				for (int i = 0; i < 4; ++i) {
					if (proc.isAlive())
						Thread.sleep(SLEEP_MILLIS);
				}
			} catch (Exception e) {
				e.printStackTrace(logger);
			}
		}
	}

	private void readAndWriteFitnesseResults(final URL readFromURL, final FilePath writeToFilePath)
			throws InterruptedException {
		final RunnerWithTimeOut runnerWithTimeOut = new RunnerWithTimeOut(builder.getFitnesseTestTimeout(envVars));

		Runnable readAndWriteResults = new Runnable() {
			public void run() {
				try {
					writeToFilePath.delete();
				} catch (Exception e) {
					// swallow - file may not exist
				}
				final byte[] bytes = getHttpBytes(readFromURL, runnerWithTimeOut, builder.getFitnesseHttpTimeout(envVars));
				writeFitnesseResults(writeToFilePath, bytes);
			}
		};

		runnerWithTimeOut.run(readAndWriteResults);
	}

	public byte[] getHttpBytes(URL pageCmdTarget, Resettable timeout, int httpTimeout) {
		InputStream inputStream = null;
		ByteArrayOutputStream bucket = new ByteArrayOutputStream();

		try {
			logger.println("Connnecting to " + pageCmdTarget);
			HttpURLConnection connection = (HttpURLConnection) pageCmdTarget.openConnection();
			connection.setReadTimeout(httpTimeout);
			logger.println("Connected: " + connection.getResponseCode() + "/" + connection.getResponseMessage());

			inputStream = connection.getInputStream();
			long recvd = 0, lastLogged = 0;
			byte[] buf = new byte[4096];
			int lastRead;
			while ((lastRead = inputStream.read(buf)) > 0) {
				bucket.write(buf, 0, lastRead);
				timeout.reset();
				recvd += lastRead;
				if (recvd - lastLogged > 1024) {
					logger.println(recvd / 1024 + "k...");
					lastLogged = recvd;
				}
			}
		} catch (IOException e) {
			// this may be a "premature EOF" caused by e.g. incorrect content-length HTTP header
			// so it may be non-fatal -- try to recover
			e.printStackTrace(logger);
		} finally {
			if (inputStream != null) {
				try {
					logger.println("Force close of input stream.");
					inputStream.close();
				} catch (Exception e) {
					logger.println("Caught exception while trying to close input stream.");
					// swallow
				}
			}
		}
		return bucket.toByteArray();
	}

	/* package for test */URL getFitnessePage(AbstractBuild<?, ?> build, boolean withCommand) throws IOException {
		return new URL("http", //
				builder.getFitnesseHost(build, envVars), //
				builder.getFitnessePort(), //
				withCommand ? getFitnessePageCmd() : getFitnessePageBase());
	}

	/* package for test */String getFitnessePageBase() {
		String targetPageExpression = builder.getFitnesseTargetPage(envVars);
		int pos = targetPageExpression.indexOf('?');
		if (pos == -1)
			pos = targetPageExpression.length();
		return "/" + targetPageExpression.substring(0, pos);
	}

	/* package for test */String getFitnessePageCmd() {
		String targetPageExpression = builder.getFitnesseTargetPage(envVars);
		if (targetPageExpression.contains("?"))
			return "/" + targetPageExpression + "&format=xml&includehtml";

		int pos = targetPageExpression.indexOf('&');
		if (pos == -1)
			pos = targetPageExpression.length();

		return String.format("/%1$s?%2$s%3$s", targetPageExpression.substring(0, pos),
				builder.getFitnesseTargetIsSuite() ? "suite" : "test", targetPageExpression.substring(pos)
						+ "&format=xml&includehtml");
	}

	private void writeFitnesseResults(FilePath resultsFilePath, byte[] results) {
		OutputStream resultsStream = null;
		try {
			resultsStream = resultsFilePath.write();
			resultsStream.write(results);
			logger.println("Xml results saved as " + Charset.defaultCharset().displayName() + " to "
					+ resultsFilePath.getRemote());
		} catch (IOException e) {
			e.printStackTrace(logger);
		} catch (InterruptedException e2) {
			e2.printStackTrace(logger);
		} finally {
			try {
				if (resultsStream != null)
					resultsStream.close();
			} catch (Exception e) {
				// swallow
			}
		}
	}

	String getAbsolutePathToFile(FilePath workingDirectory, String fileName) {
		return getFilePath(workingDirectory, fileName).getRemote();
	}

	FilePath getFilePath(FilePath workingDirectory, String fileName) {
		return getFilePath(logger, workingDirectory, fileName);
	}

	static FilePath getWorkingDirectory(PrintStream logger, AbstractBuild<?, ?> build) {
		FilePath workspace = build.getWorkspace(); // null only is slave is disconnected
		logger.println("Working directory is: " + workspace != null ? workspace.getRemote() : "null !!");
		return workspace;
	}

	static FilePath getFilePath(PrintStream logger, FilePath workingDirectory, String fileName) {
		if (workingDirectory != null) {
			FilePath fp = workingDirectory.child(fileName); // manage absolute and relative path
			try {
				if (!fp.exists()) {
					logger.printf("Can't find target file: %s with working directory: %s%n", fileName, workingDirectory);
				}
			} catch (Exception e) {
				logger.printf("Can't check if remote file exist: %s%n", e.getMessage());
			}
			return fp;
		} else { // possible ?
			logger.println("Warning: working directory is null.");
			File fileNameFile = new File(fileName); // should not work on slave if OS is different than masters' one
			return new FilePath(fileNameFile);
		}
	}
}
