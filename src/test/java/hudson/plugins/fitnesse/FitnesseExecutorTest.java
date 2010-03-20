package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.FilePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseExecutorTest {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private FitnesseExecutor executor;

	private FitnesseExecutor getExecutorForBuilder(String[] keys, String[] values) {
		Map<String, String> options = new HashMap<String, String>();
		for (int i=0; i < keys.length; ++i) {
			options.put(keys[i], values[i]);
		}
		return new FitnesseExecutor(new FitnesseBuilder(options));
	}
	
	@Test
	public void fitnesseDirShouldBeParentOfFitNesseRoot() throws IOException {
		File fitnesseRoot = File.createTempFile("child", "");
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.PATH_TO_ROOT},
				new String[] {fitnesseRoot.getAbsolutePath()});
		Assert.assertEquals(fitnesseRoot.getParentFile().getAbsolutePath(), 
				executor.getFitnesseDir());
	}

	@Test
	public void fitnesseRootShouldNotBePath() throws IOException {
		File fitnesseRoot = File.createTempFile("FitNesseRoot", "");
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.PATH_TO_ROOT},
				new String[] {fitnesseRoot.getAbsolutePath()});
		Assert.assertEquals(fitnesseRoot.getName(), 
				executor.getFitnesseRoot());
	}

	@Test
	public void javaCmdShouldIncludeJarAndDirAndRootAndPort() throws IOException {
		File fitnesseRoot = File.createTempFile("FitNesseRoot", "");
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.JAVA_OPTS, FitnesseBuilder.PATH_TO_ROOT, 
						FitnesseBuilder.PATH_TO_JAR, FitnesseBuilder.FITNESSE_PORT},
				new String[] {"", fitnesseRoot.getAbsolutePath(), 
						"fitnesseJar", "9999"});
		ArrayList<String> cmd = executor.getJavaCmd(new EnvVars());
		
		Assert.assertEquals("java", cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals("fitnesseJar", cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertEquals(executor.getFitnesseDir(), cmd.get(4));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals(executor.getFitnesseRoot(), cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9999", cmd.get(8));
	}

	@Test
	public void javaCmdShouldIncludeJavaOpts() throws IOException {
		File fitnesseRoot = File.createTempFile("FitNesseRoot", "");
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.JAVA_OPTS, FitnesseBuilder.PATH_TO_ROOT, FitnesseBuilder.PATH_TO_JAR, FitnesseBuilder.FITNESSE_PORT},
				new String[] {"-Da=b", fitnesseRoot.getAbsolutePath(), "fitnesseJar", "9999"});
		ArrayList<String> cmd = executor.getJavaCmd(new EnvVars());
		
		Assert.assertEquals("java", cmd.get(0));
		Assert.assertEquals("-Da=b", cmd.get(1));
		Assert.assertEquals("-jar", cmd.get(2));
		Assert.assertEquals("fitnesseJar", cmd.get(3));
		Assert.assertEquals("-d", cmd.get(4));
		Assert.assertEquals(executor.getFitnesseDir(), cmd.get(5));
		Assert.assertEquals("-r", cmd.get(6));
		Assert.assertEquals(executor.getFitnesseRoot(), cmd.get(7));
		Assert.assertEquals("-p", cmd.get(8));
		Assert.assertEquals("9999", cmd.get(9));
	}

	@Test
	public void javaCmdShouldReferenceJAVAHOME() throws IOException {
		File fitnesseRoot = File.createTempFile("FitNesseRoot", "");
		File javaHome = File.createTempFile("JavaHome", "");
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.PATH_TO_ROOT, FitnesseBuilder.PATH_TO_JAR, FitnesseBuilder.FITNESSE_PORT},
				new String[] {fitnesseRoot.getAbsolutePath(), "fitnesseJar", "9876"});
		
		EnvVars envVars = new EnvVars();
		envVars.put("JAVA_HOME", javaHome.getAbsolutePath());
		ArrayList<String> cmd = executor.getJavaCmd(envVars);
		
		Assert.assertEquals(new File(new File(javaHome, "bin"), "java").getAbsolutePath(), 
				cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals("fitnesseJar", cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertEquals(executor.getFitnesseDir(), cmd.get(4));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals(executor.getFitnesseRoot(), cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9876", cmd.get(8));
	}

	@Test
	public void fitnessePageCmdShouldBeTestIfPageIsNotSuite() {
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
				new String[] {"WikiPage", "false"});
		Assert.assertEquals("/WikiPage?test&format=xml", 
				executor.getFitnessePageCmd());
	}

	@Test
	public void fitnessePageCmdShouldBeSuiteIfPageIsSuite() {
		executor = getExecutorForBuilder(
			new String[] {FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
			new String[] {"WikiPage", "true"});
		Assert.assertEquals("/WikiPage?suite&format=xml", 
				executor.getFitnessePageCmd());
	}
	
	@Test
	public void fitnessePageCmdShouldReorderQueryStringIfSpecifiedInPageName() {
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
				new String[] {"WikiPage?suite&suiteFilter=tag1,tag2", "true"});
		Assert.assertEquals("/WikiPage?suite&suiteFilter=tag1,tag2&format=xml", 
				executor.getFitnessePageCmd());
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
				new String[] {"WikiPage&suiteFilter=tag1,tag2", "true"});
		Assert.assertEquals("/WikiPage?suite&suiteFilter=tag1,tag2&format=xml", 
				executor.getFitnessePageCmd());
	}
	
	@Test
	public void fitnessePageCmdURLShouldIncludeHostPortAndPageCmd() throws MalformedURLException {
		executor = getExecutorForBuilder(
				new String[] {FitnesseBuilder.FITNESSE_HOST, FitnesseBuilder.FITNESSE_PORT, FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
				new String[] {"host", "1234", "WikiPage", "true"});
		Assert.assertEquals("http://host:1234" + executor.getFitnessePageCmd(),
				executor.getFitnessePageCmdURL().toExternalForm());
	}

	@Test
	public void fitnessePageCmdURLShouldIncludeLocalHostIfStartedByHudson() throws MalformedURLException {
		executor = getExecutorForBuilder(
			new String[] {FitnesseBuilder.START_FITNESSE, FitnesseBuilder.FITNESSE_HOST, FitnesseBuilder.FITNESSE_PORT, FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE},
			new String[] {"true", "unknown_host", "8989", "WikiPage", "true"});
		Assert.assertEquals("http://localhost:8989" + executor.getFitnessePageCmd(),
				executor.getFitnessePageCmdURL().toExternalForm());
	}
	
	@Test
	public void fitnesseStartedShouldBeTrueIfStdOutHasBeenWrittenTo() throws Exception {
		executor = getExecutorForBuilder(new String[] {}, new String[] {});
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		stdout.write("Started".getBytes());
		
		PrintStream logger = new PrintStream(log);
		StdConsole console = new StdConsole(stdout, new ByteArrayOutputStream());
		console.logIncrementalOutput(logger);
		
		Assert.assertTrue(executor.fitnesseStarted(logger, console, 500));
		Assert.assertEquals(stdout.size() 
				+ LINE_SEPARATOR.getBytes().length, log.size()); 
	}

	@Test
	public void fitnesseStartedShouldBeFalseAfterTimeoutIfStdOutHasNotBeenWrittenTo() throws Exception {
		executor = getExecutorForBuilder(new String[] {}, new String[] {});
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		Assert.assertFalse(executor.fitnesseStarted(new PrintStream(stdout), 
				new StdConsole(stdout, new ByteArrayOutputStream()), 500));
		Assert.assertTrue(stdout.toString().startsWith("Waited ")); // log entry 
	}
	
	@Test
	public void getHttpBytesShouldReturnContentFromUrlAndWriteToLog() throws Exception {
		executor = getExecutorForBuilder(new String[] {}, new String[] {});
		ByteArrayOutputStream logBucket = new ByteArrayOutputStream();
		byte[] bytes = executor.getHttpBytes(new PrintStream(logBucket), new URL("http://hudson-ci.org/"));
		Assert.assertTrue(bytes.length > 0);
		Assert.assertTrue(new String(bytes).contains("<html"));
		Assert.assertTrue(new String(bytes).contains("</html>"));
		Assert.assertTrue(logBucket.toString().startsWith("Connnecting to http://hudson-ci.org/"));
		Assert.assertTrue(logBucket.toString().contains("Connected: 200/OK"));
	}
	
	@Test
	public void resultsFilePathShouldBeFileNameIfFileExists() throws Exception {
		File tmpFile = File.createTempFile("results", ".out");
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));
		
		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, tmpFile.getAbsolutePath());
		Assert.assertEquals(tmpFile.getAbsolutePath(), resultsFilePath.getRemote());
	}
	
	@Test
	public void resultsFilePathShouldBeInWorkingDirIfFileNotExists() throws Exception {
		File tmpFile = File.createTempFile("results", ".out");
		FilePath workingDirectory = new FilePath(tmpFile.getParentFile());
		
		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, "results.xml");
		Assert.assertEquals(workingDirectory.child("results.xml").getRemote(), 
							resultsFilePath.getRemote());
	}
}
