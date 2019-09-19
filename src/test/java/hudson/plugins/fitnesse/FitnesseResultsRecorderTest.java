package hudson.plugins.fitnesse;

import hudson.FilePath;
import hudson.tasks.test.TestResult;

import java.io.File;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FitnesseResultsRecorderTest {

	@Test
	public void getResultsShouldReadFromFilePath() throws Exception {
		startPlugin();
		String resultsFile = "src/test/resources/hudson/plugins/fitnesse/fitnesse-test-results.xml";
		FitnesseResultsRecorder recorder = new FitnesseResultsRecorder(resultsFile);
		FilePath resultFile = new FilePath(new File(System.getProperty("user.dir"))).child(resultsFile);
		Assert.assertNotNull(recorder.getResults(System.out, resultFile, new File("./target")));
	}

	@Test
	public void getResultsFromSuiteShouldStoreFileName() throws Exception {
		startPlugin();
		String resultsFile = "src/test/resources/hudson/plugins/fitnesse/fitnesse-suite-results.xml";
		FilePath resultsPath = new FilePath(new File(resultsFile));
		FitnesseResultsRecorder recorder = new FitnesseResultsRecorder(resultsFile);
		FitnesseResults results = recorder.getResults(System.out, resultsPath, new File("./target"));
		Assert.assertEquals("fitnesse-suite-results.xml", results.getName());
	}

	@Test
	public void getPatternResults() throws Exception {
		startPlugin();
		String resultsFile = "src/test/resources/hudson/plugins/fitnesse/fitnesse-*-results.xml";

		FitnesseResultsRecorder recorder = new FitnesseResultsRecorder(resultsFile);
		FilePath[] resultFiles = recorder.getResultFiles1(System.out, new FilePath(new File(System.getProperty("user.dir"))));
		Assert.assertNotNull(resultFiles);
		Assert.assertEquals(2, resultFiles.length);

		FitnesseResults results = recorder.getResults(System.out, resultFiles, new File("./target"));
		Assert.assertNotNull(results);
		Assert.assertTrue(results.hasChildren());
		Collection<? extends TestResult> children = results.getChildren();
		Assert.assertNotNull(children);
		Assert.assertEquals(2, children.size());
	}

	@Before
	public void startPlugin() throws Exception {
		new FitnessePlugin().start();
	}
}
