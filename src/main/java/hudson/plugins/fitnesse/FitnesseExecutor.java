package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *  
 * @author Tim Bacon
 */
public class FitnesseExecutor {
	private static final int SLEEP_MILLIS = 750;

	private static final int TIMEOUT_MILLIS = 9000;
	
	private final FitnesseBuilder builder;
	
	public FitnesseExecutor(FitnesseBuilder builder) {
		this.builder = builder;
	}

    public boolean execute(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
    throws InterruptedException {
		Proc fitnesseProc = null;
		StdConsole console = new StdConsole();
		try {
	    	if (builder.getFitnesseStart()) { 
	    		fitnesseProc = startFitnesse(launcher, build.getEnvironment(listener), console);
	    		if (!fitnesseProc.isAlive() 
	    		|| !procStarted(listener.getLogger(), console, TIMEOUT_MILLIS)) {
    				return false;
	    		}
	    		console.logOutput(listener.getLogger());
	    	}
	    	
			writeFitnesseResults(listener.getLogger(), builder.getFitnessePathToXmlResultsOut(), 
	    			getHttpBytes(listener.getLogger(), getFitnessePageCmdURL()));
	    	
	    	if (console.outputOnStdErr()) build.setResult(Result.UNSTABLE);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(listener.getLogger());
			if (t instanceof InterruptedException) throw (InterruptedException) t;
			return false;
		} finally {
			killProc(listener.getLogger(), fitnesseProc);
			console.logOutput(listener.getLogger());
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
		String[] java_opts = (builder.getFitnesseJavaOpts() == "" ? new String[0] : builder.getFitnesseJavaOpts().split(" "));
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
	
	public boolean procStarted(PrintStream log, StdConsole console, long timeout) throws InterruptedException {
		long waitedAlready = 0;
		do {
			Thread.sleep(SLEEP_MILLIS);
			waitedAlready += SLEEP_MILLIS;
		} while (console.noOutputOnStdOut() && waitedAlready < timeout) ;
		if (console.noOutputOnStdOut()) {
			log.println("Waited " + timeout + "ms for fitnesse to start.");
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
		try {
			inputStream = new BufferedInputStream(connection.getInputStream());
			ByteArrayOutputStream bucket = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int lastRead;
			while ((lastRead = inputStream.read(buf)) > 0) {
				bucket.write(buf, 0, lastRead);
			}
			return bucket.toByteArray();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
					// swallow
				}
			}
		}
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

	private void writeFitnesseResults(PrintStream log, String resultsFileName, byte[] results) throws IOException, InterruptedException {
		OutputStream resultsStream = null;
		try {
			resultsStream = new BufferedOutputStream(new FileOutputStream(resultsFileName));
			resultsStream.write(results);
			log.println("Xml results saved to " + resultsFileName);
		} finally {
			try {
				if (resultsStream != null)
					resultsStream.close();
			} catch (Exception e) {
				// swallow
			}
		}
	}
}

