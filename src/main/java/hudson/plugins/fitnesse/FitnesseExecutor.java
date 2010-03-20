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
	private static final int STARTUP_TIMEOUT_MILLIS = 10000;
	private static final int ADDITIONAL_TIMEOUT_MILLIS = 20000;
	
	private final FitnesseBuilder builder;
	
	public FitnesseExecutor(FitnesseBuilder builder) {
		this.builder = builder;
	}

    public boolean execute(AbstractBuild<?, ?> build, Launcher launcher, PrintStream logger, EnvVars environment) 
    throws InterruptedException {
		Proc fitnesseProc = null;
		StdConsole console = new StdConsole();
		try {
	    	if (builder.getFitnesseStart()) {
	    		logger.println("Starting new Fitnesse instance...");
	    		fitnesseProc = startFitnesse(launcher, environment, console);
	    		if (!procStarted(fitnesseProc, logger, console)) {
    				return false;
	    		}
	    		console.logIncrementalOutput(logger);
	    	}
	    	
			writeFitnesseResults(logger, 
								getWorkingDirectory(build),
								builder.getFitnessePathToXmlResultsOut(), 
				    			getHttpBytes(logger, getFitnessePageCmdURL()));
	    	
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

	private Proc startFitnesse(Launcher launcher, EnvVars envVars, StdConsole console) throws IOException {
    	ProcStarter procStarter = launcher.launch().cmds(getJavaCmd(envVars));
    	console.provideStdOutAndStdErrFor(procStarter);
		return procStarter.start();
    }

	public ArrayList<String> getJavaCmd(EnvVars envVars) {
		String java = "java"; 
		if (envVars.containsKey("JAVA_HOME"))
			java = new File(new File(envVars.get("JAVA_HOME"), "bin"), java).getAbsolutePath();
		String fitnesseJavaOpts = builder.getFitnesseJavaOpts();
		String[] java_opts = ("".equals(fitnesseJavaOpts) ? new String[0] : fitnesseJavaOpts.split(" "));
		String[] jar_opts = {"-jar", builder.getFitnessePathToJar()};
		String[] fitnesse_opts = {"-d", getFitnesseDir(), 
				"-r", getFitnesseRoot(), 
				"-p", Integer.toString(builder.getFitnessePort())};
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(java);
		if (java_opts.length > 0) cmd.addAll(Arrays.asList(java_opts));
		cmd.addAll(Arrays.asList(jar_opts));
		cmd.addAll(Arrays.asList(fitnesse_opts));
		
		return cmd;
	}
    
	public String getFitnesseDir() {
		return new File(builder.getFitnessePathToRoot()).getParentFile().getAbsolutePath();
	}
	
	public String getFitnesseRoot() {
		return new File(builder.getFitnessePathToRoot()).getName();
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
	
	public byte[] getHttpBytes(PrintStream log, URL pageCmdTarget) throws MalformedURLException, IOException {
		log.println("Connnecting to " + pageCmdTarget);
		HttpURLConnection connection = (HttpURLConnection) pageCmdTarget.openConnection();
		log.println("Connected: " + connection.getResponseCode() + "/" + connection.getResponseMessage());
		InputStream inputStream = null;
		ByteArrayOutputStream bucket = new ByteArrayOutputStream();
		try {
			inputStream = connection.getInputStream();
			int reads = 0, lastLogged = 0;
			byte[] buf = new byte[1024];
			int lastRead;
			while ((lastRead = inputStream.read(buf)) > 0) {
				bucket.write(buf, 0, lastRead);
				reads += lastRead;
				if (reads - lastLogged > 1024) {
					log.println(reads + " bytes...");
					lastLogged = reads;
				}
			}
		} catch (IOException e) {
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
				builder.getFitnesseStart() ? "localhost" : builder.getFitnesseHost(), 
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

	private void writeFitnesseResults(PrintStream log, FilePath workingDirectory, String resultsFileName, byte[] results) throws IOException, InterruptedException {
		OutputStream resultsStream = null;
		try {
			FilePath resultsFilePath = getResultsFilePath(workingDirectory, resultsFileName);
			resultsStream = resultsFilePath.write();
			resultsStream.write(results);
			log.println("Xml results saved as " + Charset.defaultCharset().displayName()
					+ " to " + resultsFilePath.getRemote());
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
		if (workspace != null) return workspace;
		return new FilePath(build.getRootDir());
	}

	static FilePath getResultsFilePath(FilePath workingDirectory, String fileName) {
		File fileNameFile = new File(fileName);
		if (fileNameFile.exists()) return new FilePath(fileNameFile);
		return workingDirectory.child(fileName);
	}
}

