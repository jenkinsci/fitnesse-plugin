package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

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

/**
 *  
 * @author Tim Bacon
 */
public class FitnesseExecutor {
	private static final int SLEEP_MILLIS = 1000;
  private static final int STARTUP_TIMEOUT_MILLIS = 30 * 1000;
	
	private final FitnesseBuilder builder;
	
	public FitnesseExecutor(FitnesseBuilder builder) {
		this.builder = builder;
	}

    public boolean execute(AbstractBuild<?, ?> build, Launcher launcher, PrintStream logger, EnvVars environment) 
    throws InterruptedException {
		Proc fitnesseProc = null;
		try {
			build.addAction(getFitnesseBuildAction(build, environment));
	    	if (builder.getFitnesseStart()) {
				fitnesseProc = startFitnesse(build, launcher, environment, logger);
				if (!procStarted(fitnesseProc, logger)) {
    				return false;
	    		}
	    	}
	    	
	    	FilePath resultsFilePath = getResultsFilePath(getWorkingDirectory(build), 
	    												builder.getFitnessePathToXmlResultsOut(environment));
			readAndWriteFitnesseResults(logger,
			    getFitnessePageCmdURL(build, environment), resultsFilePath,
			    environment);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(logger);
			if (t instanceof InterruptedException)
				throw (InterruptedException) t;
			return false;
		} finally {
			killProc(logger, fitnesseProc);
		}
	}

	private FitnesseBuildAction getFitnesseBuildAction(AbstractBuild<?,?> build, EnvVars environment) throws InterruptedException, IOException {
		return new FitnesseBuildAction(
				builder.getFitnesseStart(),
				builder.getFitnesseHost(build, environment), 
				builder.getFitnessePort());
	}

	private Proc startFitnesse(AbstractBuild<?, ?> build, Launcher launcher,
	    EnvVars envVars, PrintStream logger) throws IOException {
		logger.println("Starting new Fitnesse instance...");
		ProcStarter procStarter = launcher.launch().cmds(getJavaCmd(getWorkingDirectory(build), envVars));
		procStarter.pwd(new File(getAbsolutePathToFileThatMayBeRelativeToWorkspace(getWorkingDirectory(build), builder.getFitnesseJavaWorkingDirectory())));
		procStarter.stdout(logger).stderr(logger);
		return procStarter.start();
    }

	public ArrayList<String> getJavaCmd(FilePath workingDirectory, EnvVars envVars) {
		String java = "java"; 
		if(!builder.getFitnesseJdk(envVars).isEmpty()){
		   File customJavaHome = Hudson.getInstance().getJDK(builder.getFitnesseJdk(envVars)).getBinDir();
		   java = new File(customJavaHome, java).getAbsolutePath();
		} else if (envVars.containsKey("JAVA_HOME")) {
			java = new File(new File(envVars.get("JAVA_HOME"), "bin"), java).getAbsolutePath();
		}
		String fitnesseJavaOpts = builder.getFitnesseJavaOpts(envVars);
		String[] java_opts = ("".equals(fitnesseJavaOpts) ? new String[0] : fitnesseJavaOpts.split(" "));

		String absolutePathToFitnesseJar = getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, builder.getFitnessePathToJar());
		String[] jar_opts = {"-jar", absolutePathToFitnesseJar};
		
		File fitNesseRoot = new File(getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, builder.getFitnessePathToRoot()));
		String[] fitnesse_opts = {"-d", fitNesseRoot.getParent(), 
				"-r", fitNesseRoot.getName(), 
				"-p", Integer.toString(builder.getFitnessePort())};
		
		// split additional fitness options and add them to those explicitly configured ones
		String[] addOps = splitOptions(builder.getAdditionalFitnesseOptions());
		
		String[] fitnesse_opts2 = new String[fitnesse_opts.length
				+ addOps.length];
		System.arraycopy(fitnesse_opts, 0, fitnesse_opts2, 0,
				fitnesse_opts.length);
		System.arraycopy(addOps, 0, fitnesse_opts2, fitnesse_opts.length,
				addOps.length);
	
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(java);
		if (java_opts.length > 0)
			cmd.addAll(Arrays.asList(java_opts));
		cmd.addAll(Arrays.asList(jar_opts));
		cmd.addAll(Arrays.asList(fitnesse_opts2));
		
		return cmd;
	}
	
	/**
	 * Breaks the given string down by any options of the form "-x" or "-x some argument not containing a - character"
	 */
	private static String[] splitOptions(String string) {
		List<String> addOps = new ArrayList<String>(); 
		
		// match pattern to identify additional cmd arguments
		Pattern pattern = Pattern.compile("-{1}[a-z]{1}\\s?[^-]*");
		Matcher m = pattern.matcher(string);
		
		while (m.find()) {
		    String s = m.group();
		    addOps.add(s.substring(0,2).trim());
		    addOps.add(s.substring(2,s.length()).trim());
		}
		String[] ret = new String[addOps.size()];
		return addOps.toArray(ret);
	}

	private boolean procStarted(Proc fitnesseProc, PrintStream log)
	    throws IOException, InterruptedException {
		if (fitnesseProc.isAlive()) {
			return fitnesseStarted(log, builder.getFitnessePort());
		}
		return false;
	}
	
	/**
	 * Detect if fitnesse has started by check the port availability
	 * 
	 * @return true if fitnesse has started, false otherwise
	 */
	public boolean fitnesseStarted(final PrintStream log, int port)
	    throws InterruptedException {
		final String url = "http://localhost:" + port + "/";

		long waitedAlready;
		boolean launched = false;
		for (waitedAlready = 0; waitedAlready < STARTUP_TIMEOUT_MILLIS; waitedAlready += SLEEP_MILLIS) {
			Thread.sleep(SLEEP_MILLIS);
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) new URL(url).openConnection();
				connection.setRequestMethod("GET");
				launched = connection.getResponseCode() == 200;
				break;
			} catch (IOException e) {
				// swallow exception
			} finally {
				if (connection != null)
					connection.disconnect(); // TOAA : à faire ??
			}
		}

		if (!launched)
			log.println("Waited " + waitedAlready + "ms for fitnesse to start.");

		return launched;
	}

	private void killProc(PrintStream log, Proc proc) {
		if (proc != null) {
			try {
				proc.kill();
				for (int i=0; i < 4; ++i) {
					if (proc.isAlive())
						Thread.sleep(SLEEP_MILLIS);
				}
			} catch (Exception e) {
				e.printStackTrace(log);
			}
		}
	}
	
	private void readAndWriteFitnesseResults(final PrintStream logger,
	    final URL readFromURL, final FilePath writeToFilePath,
	    final EnvVars environment)
	throws InterruptedException {
		final RunnerWithTimeOut runnerWithTimeOut = new RunnerWithTimeOut(builder.getFitnesseTestTimeout(environment));
	
		Runnable readAndWriteResults = new Runnable() {
			public void run() {
				try {
					writeToFilePath.delete();
				} catch (Exception e) {
					// swallow - file may not exist
				}
				final byte[] bytes = getHttpBytes(logger, readFromURL, runnerWithTimeOut,
						builder.getFitnesseHttpTimeout(environment));
				writeFitnesseResults(logger, writeToFilePath, bytes); 
			}
		};
		
		
		runnerWithTimeOut.run(readAndWriteResults);
	}
	
	public byte[] getHttpBytes(PrintStream log, URL pageCmdTarget, Resettable timeout, int httpTimeout) {
		InputStream inputStream = null;
		ByteArrayOutputStream bucket = new ByteArrayOutputStream();

		try {
			log.println("Connnecting to " + pageCmdTarget);
			HttpURLConnection connection = (HttpURLConnection) pageCmdTarget.openConnection();
			connection.setReadTimeout(httpTimeout);
			log.println("Connected: " + connection.getResponseCode() + "/" + connection.getResponseMessage());

			inputStream = connection.getInputStream();
			long recvd = 0, lastLogged = 0;
			byte[] buf = new byte[4096];
			int lastRead;
			while ((lastRead = inputStream.read(buf)) > 0) {
				bucket.write(buf, 0, lastRead);
				timeout.reset();
				recvd += lastRead;
				if (recvd - lastLogged > 1024) {
					log.println(recvd/1024 + "k...");
					lastLogged = recvd;
				}
			}
		} catch (IOException e) {
			// this may be a "premature EOF" caused by e.g. incorrect content-length HTTP header
			// so it may be non-fatal -- try to recover
			e.printStackTrace(log);
		} finally {
			if (inputStream != null) {
				try {
					log.println("Force close of input stream.");
					inputStream.close();
				} catch (Exception e) {
					log.println("Caught exception while trying to close input stream.");
					// swallow
				}
			}
		}
		return bucket.toByteArray();
	}

	public URL getFitnessePageCmdURL(AbstractBuild<?,?> build, EnvVars environment) throws Exception {
		return new URL("http", 
				builder.getFitnesseHost(build, environment), 
				builder.getFitnessePort(), 
				getFitnessePageCmd(environment));
	}
	

	public String getFitnessePageCmd(EnvVars environment) {
		String targetPageExpression = builder.getFitnesseTargetPage(environment);
		if (targetPageExpression.contains("?"))
			return "/" + targetPageExpression + "&format=xml&includehtml";
		
		int pos = targetPageExpression.indexOf('&');
		if (pos == -1)
			pos = targetPageExpression.length();
		
		return String.format("/%1$s?%2$s%3$s", 
				targetPageExpression.substring(0, pos),
				builder.getFitnesseTargetIsSuite() ? "suite" : "test",
				targetPageExpression.substring(pos)+"&format=xml&includehtml");
	}

	private void writeFitnesseResults(PrintStream log, FilePath resultsFilePath, byte[] results) {
		OutputStream resultsStream = null;
		try {
			resultsStream = resultsFilePath.write();
			resultsStream.write(results);
			log.println("Xml results saved as " + Charset.defaultCharset().displayName()
					+ " to " + resultsFilePath.getRemote());
		} catch (IOException e) {
			e.printStackTrace(log);
		} catch (InterruptedException e2) {
			e2.printStackTrace(log);
		} finally {
			try {
				if (resultsStream != null)
					resultsStream.close();
			} catch (Exception e) {
				// swallow
			}
		}
	}

	static FilePath getWorkingDirectory(AbstractBuild<?, ?> build) {
		FilePath workspace = build.getWorkspace();
		if (workspace != null)
			return workspace;
		return new FilePath(build.getRootDir());
	}
	
	static FilePath getResultsFilePath(FilePath workingDirectory, String fileName) {
		File fileNameFile = new File(fileName);
		
		if (fileNameFile.getParent() != null) {
			if (fileNameFile.exists() || fileNameFile.getParentFile().exists()) {
				return new FilePath(fileNameFile);
			}
		}
		
		return workingDirectory.child(fileName);
	}
	
	static String getAbsolutePathToFileThatMayBeRelativeToWorkspace(FilePath workingDirectory, String fileName) {
		if (new File(fileName).exists())
			return fileName;
		return new File(workingDirectory.getRemote(), fileName).getAbsolutePath();
	}
}
