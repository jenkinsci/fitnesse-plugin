package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.FilePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FitnesseExecutorTest {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
	
	private FitnesseExecutor executor;
	private ByteArrayOutputStream output = new ByteArrayOutputStream();

	private void init(String[] keys, String[] values) {
		Map<String, String> options = new HashMap<String, String>();
		for (int i=0; i < keys.length; ++i) {
			options.put(keys[i], values[i]);
		}
		output.reset();
		executor = new FitnesseExecutor(new FitnesseBuilder(options), new PrintStream(output));
	}

	@Test
	public void javaCmdShouldIncludeJarAndDirAndRootAndPort() throws IOException {
		init(new String[] { FitnesseBuilder.JAVA_OPTS, FitnesseBuilder.PATH_TO_ROOT,
		    FitnesseBuilder.PATH_TO_JAR, FitnesseBuilder.FITNESSE_PORT }, new String[] { "",
		    getTestResourceFitNesseRoot(), getTestResourceFitnesseJar(), "9999" });
		FilePath workingDirectory = new FilePath(new File(TMP_DIR));
		ArrayList<String> cmd = executor.getJavaCmd(workingDirectory, new EnvVars());
		
		Assert.assertEquals("java", cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals(getTestResourceFitnesseJar(), cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertEquals(new File(getTestResourceFitNesseRoot()).getParent(), cmd.get(4));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals("FitNesseRoot", cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9999", cmd.get(8));
	}

	private static String getTestResourceFitnesseJar() {
		return new File(new File(System.getProperty("user.dir")), 
				"target/test-classes/fitnesse.jar").getAbsolutePath();
	}
	
	private static String getTestResourceFitNesseRoot() {
		return new File(new File(System.getProperty("user.dir")), 
		"target/test-classes/FitNesseRoot").getAbsolutePath();
	}

	@Test
	public void javaCmdShouldIncludeJavaOpts() throws IOException {
		init(new String[] { FitnesseBuilder.JAVA_OPTS, FitnesseBuilder.PATH_TO_ROOT,
		    FitnesseBuilder.PATH_TO_JAR, FitnesseBuilder.FITNESSE_PORT }, new String[] { "-Da=b",
		    getTestResourceFitNesseRoot(), getTestResourceFitnesseJar(), "9999" });

		FilePath workingDirectory = new FilePath(new File(TMP_DIR));
		ArrayList<String> cmd = executor.getJavaCmd(workingDirectory, new EnvVars());
		
		Assert.assertEquals("java", cmd.get(0));
		Assert.assertEquals("-Da=b", cmd.get(1));
		Assert.assertEquals("-jar", cmd.get(2));
		Assert.assertEquals(getTestResourceFitnesseJar(), cmd.get(3));
		Assert.assertEquals("-d", cmd.get(4));
		Assert.assertEquals(new File(getTestResourceFitNesseRoot()).getParent(), cmd.get(5));
		Assert.assertEquals("-r", cmd.get(6));
		Assert.assertEquals("FitNesseRoot", cmd.get(7));
		Assert.assertEquals("-p", cmd.get(8));
		Assert.assertEquals("9999", cmd.get(9));
	}

	@Test
	public void javaCmdShouldReferenceJAVAHOME() throws IOException {
		File javaHome = File.createTempFile("JavaHome", "");
		init(new String[] { FitnesseBuilder.PATH_TO_ROOT, FitnesseBuilder.PATH_TO_JAR,
		    FitnesseBuilder.FITNESSE_PORT },
				new String[] {getTestResourceFitNesseRoot(), getTestResourceFitnesseJar(), "9876"});
		
		EnvVars envVars = new EnvVars();
		envVars.put("JAVA_HOME", javaHome.getAbsolutePath());
		FilePath workingDirectory = new FilePath(new File(TMP_DIR));
		ArrayList<String> cmd = executor.getJavaCmd(workingDirectory, envVars);
		
		Assert.assertEquals(new File(new File(javaHome, "bin"), "java").getAbsolutePath(), 
				cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals(getTestResourceFitnesseJar(), cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertEquals(new File(getTestResourceFitNesseRoot()).getParent(), cmd.get(4));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals("FitNesseRoot", cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9876", cmd.get(8));
	}
	
	@Test
	@Ignore
	public void javaCmdShouldReferenceFitnesseSpecificJavaHome() throws IOException {
		File javaHome = File.createTempFile("JavaHome", "");
		init(new String[] { FitnesseBuilder.PATH_TO_ROOT, FitnesseBuilder.PATH_TO_JAR,
				FitnesseBuilder.FITNESSE_PORT, FitnesseBuilder.FITNESSE_JDK },
				new String[] { getTestResourceFitNesseRoot(), getTestResourceFitnesseJar(), "9876", javaHome.getAbsolutePath() });

		EnvVars envVars = new EnvVars();
		FilePath workingDirectory = new FilePath(new File(TMP_DIR));
		ArrayList<String> cmd = executor.getJavaCmd(workingDirectory, envVars);

		Assert.assertEquals(new File(new File(javaHome, "bin"), "java").getAbsolutePath(), cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals(getTestResourceFitnesseJar(), cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertEquals(new File(getTestResourceFitNesseRoot()).getParent(), cmd.get(4));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals("FitNesseRoot", cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9876", cmd.get(8));
	}

	@Test
	public void javaCmdShouldHandleRelativePaths() throws IOException {
		init(new String[] { FitnesseBuilder.PATH_TO_ROOT, FitnesseBuilder.PATH_TO_JAR,
		    FitnesseBuilder.FITNESSE_PORT },
				new String[] {"FitNesseRoot", "fitnesse.jar", "9000"});
		
		EnvVars envVars = new EnvVars();
		FilePath workingDirectory = new FilePath(new File(TMP_DIR));
		ArrayList<String> cmd = executor.getJavaCmd(workingDirectory, envVars);

		Assert.assertEquals("java", cmd.get(0));
		Assert.assertEquals("-jar", cmd.get(1));
		Assert.assertEquals(new File(TMP_DIR, "fitnesse.jar").getAbsolutePath(), cmd.get(2));
		Assert.assertEquals("-d", cmd.get(3));
		Assert.assertTrue(TMP_DIR.contains(cmd.get(4)));
		Assert.assertEquals("-r", cmd.get(5));
		Assert.assertEquals("FitNesseRoot", cmd.get(6));
		Assert.assertEquals("-p", cmd.get(7));
		Assert.assertEquals("9000", cmd.get(8));
	}

	@Test
	public void fitnessePageBase() {
		init(new String[] { FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE },
				new String[] { "WikiPage", "true" });
		Assert.assertEquals("/WikiPage", executor.getFitnessePageBase(null));
	}

	@Test
	public void fitnessePageCmdShouldBeTestIfPageIsNotSuite() {
		init(new String[] { FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE },
				new String[] {"WikiPage", "false"});
		Assert.assertEquals("/WikiPage?test&format=xml&includehtml",
				executor.getFitnessePageCmd(null));
	}

	@Test
	public void fitnessePageCmdShouldBeSuiteIfPageIsSuite() {
		init(new String[] { FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE },
			new String[] {"WikiPage", "true"});
		Assert.assertEquals("/WikiPage?suite&format=xml&includehtml", 
				executor.getFitnessePageCmd(null));
	}

	@Test
	public void fitnessePageCmdShouldReorderQueryStringIfSpecifiedInPageName() {
		init(new String[] { FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE },
				new String[] {"WikiPage?suite&suiteFilter=tag1,tag2", "true"});
		Assert.assertEquals("/WikiPage?suite&suiteFilter=tag1,tag2&format=xml&includehtml", 
				executor.getFitnessePageCmd(null));

		init(new String[] { FitnesseBuilder.TARGET_PAGE, FitnesseBuilder.TARGET_IS_SUITE },
				new String[] {"WikiPage&suiteFilter=tag1,tag2", "true"});
		Assert.assertEquals("/WikiPage?suite&suiteFilter=tag1,tag2&format=xml&includehtml", 
				executor.getFitnessePageCmd(null));
	}
	
	@Test
	public void fitnesseStartedShouldBeTrue() throws Exception {
		init(new String[] {}, new String[] {});
		
		Assert.assertTrue(executor.isFitnesseStarted(new URL("http://hudson-ci.org/")));
		Assert.assertTrue(output.toString().contains("Fitnesse server started "));
	}

	@Test
	public void fitnesseStartedShouldBeFalseAfterTimeout() throws Exception {
		init(new String[] {}, new String[] {});

		Assert.assertFalse(executor.isFitnesseStarted(new URL("http://hudson-ci.error/")));
		Assert.assertTrue(output.toString().contains("Fitnesse server NOT started "));
	}

	private boolean resetWasCalled;
	@Test
	public void getHttpBytesShouldReturnContentFromUrlWriteToLogAndCallReset() throws Exception {
		init(new String[] {}, new String[] {});
		resetWasCalled = false;
		Resettable resettable = new Resettable() {
			public void reset() { resetWasCalled = true; }
		};
		byte[] bytes = executor.getHttpBytes(new URL("http://hudson-ci.org/"), resettable, 60 * 1000);
		Assert.assertTrue(bytes.length > 0);
		Assert.assertTrue(new String(bytes).contains("<html"));
		Assert.assertTrue(new String(bytes).contains("</html>"));
		Assert.assertTrue(output.toString().startsWith("Connnecting to http://hudson-ci.org/"));
		Assert.assertTrue(output.toString().contains("Connected: 200/OK"));
		Assert.assertTrue(resetWasCalled);
	}
	
	@Test
	public void resultsFilePathShouldBeFileNameIfFileExists() throws Exception {
		File tmpFile = File.createTempFile("results", ".out");
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));
		
		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, tmpFile.getAbsolutePath());
		Assert.assertEquals(tmpFile.getAbsolutePath(), resultsFilePath.getRemote());
	}

	@Test
	public void resultsFilePathShouldBeFileNameIfParentFileExists() throws Exception {
		File tmpFile = File.createTempFile("results", ".out");
		File xmlFile = new File(tmpFile.getParentFile(), System.currentTimeMillis() + "results.xml");
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));

		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, xmlFile.getAbsolutePath());
		Assert.assertEquals(xmlFile.getAbsolutePath(), resultsFilePath.getRemote());
	}
	
	@Test
	public void resultsFilePathShouldBeInWorkingDirIfFileNotExists() throws Exception {
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));
		String noSuchFileName = "noSuchFile" + System.currentTimeMillis();
		
		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, noSuchFileName);
		Assert.assertEquals(workingDirectory.child(noSuchFileName).getRemote(), 
							resultsFilePath.getRemote());
	}

	@Test
	public void resultsFilePathShouldBeInWorkingDirIfParentFileNotExists() throws Exception {
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));
		File xmlFile = new File("noSuchDirectory" + System.currentTimeMillis(), "results.xml");
		
		FilePath resultsFilePath = FitnesseExecutor.getResultsFilePath(workingDirectory, xmlFile.getPath());
		Assert.assertEquals(workingDirectory.child(xmlFile.getPath()).getRemote(), 
				resultsFilePath.getRemote());
	}
	
	@Test
	public void absolutePathForFileThatExistsShouldBeFilePath() throws Exception {
		FilePath workingDirectory = new FilePath(new File(System.getProperty("user.home")));
		File tmpFile = File.createTempFile("fitnesse", ".jar");
		Assert.assertEquals(tmpFile.getAbsolutePath(), 
				FitnesseExecutor.getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, tmpFile.getAbsolutePath()));
	}
	
	@Test
	public void absolutePathForFileThatDoesntExistShouldBeRelative() throws Exception {
		File localPath = new File(System.getProperty("user.home"));
		FilePath workingDirectory = new FilePath(localPath);
		String relativePath = "fitnesse.jar";
		Assert.assertEquals(new File(localPath, relativePath).getAbsolutePath(), 
				FitnesseExecutor.getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, relativePath));
		relativePath = "jars/fitnesse.jar";
		Assert.assertEquals(new File(localPath, relativePath).getAbsolutePath(), 
				FitnesseExecutor.getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, relativePath));
		relativePath = "/jars/fitnesse.jar";
		Assert.assertEquals(new File(localPath, relativePath).getAbsolutePath(), 
				FitnesseExecutor.getAbsolutePathToFileThatMayBeRelativeToWorkspace(workingDirectory, relativePath));
	}
}
