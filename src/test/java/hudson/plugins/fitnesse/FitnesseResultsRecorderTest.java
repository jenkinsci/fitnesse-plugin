package hudson.plugins.fitnesse;

import hudson.FilePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FitnesseResultsRecorderTest {

	@Test
	public void getResultsShouldReadFromFilePath() throws Exception {
		startPlugin();
		String resultsFile = "src/test/resources/hudson/plugins/fitnesse/fitnesse-test-results.xml";
		FitnesseResultsRecorder recorder = new FitnesseResultsRecorder(resultsFile);
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		Assert.assertNotNull(recorder.getResults(new PrintStream(log), 
				new FilePath(new File(System.getProperty("user.dir"))).child(resultsFile)));
	}

	@Before
	public void startPlugin() throws Exception {
		new FitnessePlugin().start();
	}
}
