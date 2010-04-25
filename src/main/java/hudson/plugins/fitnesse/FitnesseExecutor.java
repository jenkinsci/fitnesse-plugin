package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *  
 * @author Tim Bacon
 */
public class FitnesseExecutor {
	private static final int SLEEP_MILLIS = 1000;
	private static final int STARTUP_TIMEOUT_MILLIS = 10*1000;
	private static final int ADDITIONAL_TIMEOUT_MILLIS = 20*1000;
	
	private final FitnesseBuilder builder;
	
	public FitnesseExecutor(FitnesseBuilder builder) {
		this.builder = builder;
	}

    public boolean execute(AbstractBuild<?, ?> build, Launcher launcher, PrintStream logger, EnvVars environment) 
    throws InterruptedException {
		Proc fitnesseProc = null;
		StdConsole console = new StdConsole();
		build.addAction(getFitnesseBuildAction());
		try {
	    	if (builder.getFitnesseStart()) {
	    		fitnesseProc = startFitnesse(build, launcher, environment, logger, console);
	    		if (!procStarted(fitnesseProc, logger, console)) {
    				return false;
	    		}
	    		console.logIncrementalOutput(logger);
	    	}
	    	
	    	FilePath resultsFilePath = getResultsFilePath(getWorkingDirectory(build), 
	    												builder.getFitnessePathToXmlResultsOut());
			readAndWriteFitnesseResults(logger, console, getFitnessePageCmdURL(), resultsFilePath);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(logger);
			if (t instanceof InterruptedException) throw (InterruptedException) t;
			return false;
		} finally {
			killProc(logger, fitnesseProc);
			console.logIncrementalOutput(logger);
		}
	}

	private FitnesseBuildAction getFitnesseBuildAction() {
		return new FitnesseBuildAction(
				builder.getFitnesseStart(),
				builder.getFitnesseHost(), 
				builder.getFitnessePort());
	}

	private Proc startFitnesse(AbstractBuild<?,?> build, Launcher launcher, EnvVars envVars, PrintStream logger, StdConsole console) throws IOException {
		logger.println("Starting new Fitnesse instance...");
		ProcStarter procStarter = launcher.launch().cmds(getJavaCmd(getWorkingDirectory(build), envVars));
		procStarter.pwd(new File(getAbsolutePathToFileThatMayBeRelativeToWorkspace(getWorkingDirectory(build), builder.getFitnesseJavaWorkingDirectory())));
    	console.provideStdOutAndStdErrFor(procStarter);
		return procStarter.start();
    }

	public ArrayList<String> getJavaCmd(FilePath workingDirectory, EnvVars envVars) {
		String java = "java"; 
		if (envVars.containsKey("JAVA_HOME"))
			java = new File(new File(envVars.get("JAVA_HOME"), "bin"), java).getAbsolutePath();
		String fitnesseJavaOpts = builder.getFitnesseJavaOpts();
		String[] java_opts = ("".equals(fitnesseJavaOpts) ? new String[0] : fitnesseJavaOpts.split(" "));

		String absolutePathToFitnesseJar = getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, builder.getFitnessePathToJar());
		String[] jar_opts = {"-jar", absolutePathToFitnesseJar};
		
		File fitNesseRoot = new File(getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, builder.getFitnessePathToRoot()));
		String[] fitnesse_opts = {"-d", fitNesseRoot.getParent(), 
				"-r", fitNesseRoot.getName(), 
				"-p", Integer.toString(builder.getFitnessePort())};
	
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(java);
		if (java_opts.length > 0) cmd.addAll(Arrays.asList(java_opts));
		cmd.addAll(Arrays.asList(jar_opts));
		cmd.addAll(Arrays.asList(fitnesse_opts));
		
		return cmd;
	}
    
	private boolean procStarted(Proc fitnesseProc, PrintStream log, StdConsole console) throws IOException, InterruptedException {
		if (fitnesseProc.isAlive()) {
			return fitnesseStarted(log, console, STARTUP_TIMEOUT_MILLIS);
		}
		return false;
	}
	
	/**
	 * Detect if fitnesse has started by monitoring the console.
	 * If fitnesse.jar is unpacking itself there will be an initial write
	 * to stderr followed by multiple writes to stdout, otherwise there 
	 * should only be an initial short write to stdout (and any writes to stderr
	 * are probably exception messages.) 
	 * @return true if fitnesse has started, false otherwise
	 */
	public boolean fitnesseStarted(PrintStream log, StdConsole console, long timeout) throws InterruptedException {
		long waitedAlready = 0;
		do {
			Thread.sleep(SLEEP_MILLIS);
			if (console.noIncrementalOutput()) {
				waitedAlready += SLEEP_MILLIS;
			} else {
				if (console.incrementalOutputOnStdErr()) 
					timeout += ADDITIONAL_TIMEOUT_MILLIS;
				console.logIncrementalOutput(log);
			}
		} while (waitedAlready < timeout) ;

		if (console.noOutputOnStdOut()) {
			log.println("Waited " + waitedAlready + "ms for fitnesse to start.");
			return false;
		}
		return true;
	}

	private void killProc(PrintStream log, Proc proc) {
		if (proc != null) {
			try {
				proc.kill();
				for (int i=0; i < 4; ++i) {
					if (proc.isAlive()) Thread.sleep(SLEEP_MILLIS);
				}
			} catch (Exception e) {
				e.printStackTrace(log);
			}
		}
	}
	
	private void readAndWriteFitnesseResults(final PrintStream logger, final StdConsole console,
											final URL readFromURL, final FilePath writeToFilePath)	
	throws InterruptedException {
		final RunnerWithTimeOut runnerWithTimeOut = new RunnerWithTimeOut(builder.getFitnesseHttpTimeout());
	
		Runnable readAndWriteResults = new Runnable() {
			public void run() {
				try {
					writeToFilePath.delete();
				} catch (Exception e) {
					// swallow - file may not exist
				}
				final byte[] bytes = getHttpBytes(logger, readFromURL, runnerWithTimeOut);
				writeFitnesseResults(logger, writeToFilePath, bytes); 
			}
		};
		
		ResetEvent logToConsole = new ResetEvent() {
			public void onReset() {
				console.logIncrementalOutput(logger);
			}
		};
		
		runnerWithTimeOut.run(readAndWriteResults, logToConsole);
	}
	
	public byte[] getHttpBytes(PrintStream log, URL pageCmdTarget, Resettable timeout) {
		InputStream inputStream = null;
		ByteArrayOutputStream bucket = new ByteArrayOutputStream();

		try {
			log.println("Connnecting to " + pageCmdTarget);
			HttpURLConnection connection = (HttpURLConnection) pageCmdTarget.openConnection();
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
					inputStream.close();
				} catch (Exception e) {
					// swallow
				}
			}
		}
		return bucket.toByteArray();
	}

	public URL getFitnessePageCmdURL() throws MalformedURLException {
		return new URL("http", 
				builder.getFitnesseHost(), 
				builder.getFitnessePort(), 
				getFitnessePageCmd());
	}

	public String getFitnessePageCmd() {
		String targetPageExpression = builder.getFitnesseTargetPage();
		if (targetPageExpression.contains("?"))
			return "/" + targetPageExpression+"&format=xml";
		
		int pos = targetPageExpression.indexOf('&');
		if (pos == -1) pos = targetPageExpression.length();
		
		return String.format("/%1$s?%2$s%3$s", 
				targetPageExpression.substring(0, pos),
				builder.getFitnesseTargetIsSuite() ? "suite" : "test",
				targetPageExpression.substring(pos)+"&format=xml");
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
				if (resultsStream != null) resultsStream.close();
			} catch (Exception e) {
				// swallow
			}
		}
	}

	static FilePath getWorkingDirectory(AbstractBuild<?, ?> build) {
		FilePath workspace = build.getWorkspace();
		if (workspace != null) return workspace;
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
		if (new File(fileName).exists()) return fileName;
		return new File(workingDirectory.getRemote(), fileName).getAbsolutePath();
	}
}
