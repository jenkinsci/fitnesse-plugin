package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.FitnesseResultsRecorder.DescriptorImpl;
import hudson.util.FormValidation.Kind;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class RecorderDescriptorImplTest {

	private DescriptorImpl descriptor;

	public RecorderDescriptorImplTest() {
		descriptor = new DescriptorImpl();
	}
	
	@Test
	public void emptyPathtoXmlResultsShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePathToXmlResultsIn("").kind);
	}
	
	public void nonExistentFitnesseResultsShouldBeOK() throws Exception {
		Assert.assertEquals(Kind.OK, 
			descriptor.doCheckFitnessePathToXmlResultsIn("aldhfashf.xml").kind);
	}
	
	@Test
	public void incorrectlyEndedFitnesseResultsShouldBeWarning() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-results", "");
		Assert.assertEquals(Kind.WARNING, 
			descriptor.doCheckFitnessePathToXmlResultsIn(tmpFile.getAbsolutePath()).kind);
	}

	@Test
	public void correctlyEndedFitnesseResultsShouldBeOk() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-results", "xml");
		Assert.assertEquals(Kind.OK, 
				descriptor.doCheckFitnessePathToXmlResultsIn(tmpFile.getAbsolutePath()).kind);
	}
	
}
