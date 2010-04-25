package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.FitnesseBuilder.DescriptorImpl;
import hudson.util.FormValidation.Kind;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class BuilderDescriptorImplTest {
	private DescriptorImpl descriptor;
	
	public BuilderDescriptorImplTest() {
		descriptor = new DescriptorImpl();
	}

	@Test
	public void emptyHostShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnesseHost("").kind);
	}

	@Test
	public void emptyPortShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePort("").kind);
	}

	@Test
	public void nonNumericPortShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePort("a").kind);
	}

	@Test
	public void negativePortShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePort("-1").kind);
	}

	@Test
	public void emptyPathToFitnesseJarShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePathToJar("").kind);
	}
	
	@Test
	public void incorrectlyEndedFitnesseJarShouldBeWarning() throws Exception {
		File tmpFile = File.createTempFile("fitnesse", "notDotJar");
		File fitnessedotJar = new File(tmpFile.getParentFile(), "fitnesse.jar");
		fitnessedotJar.createNewFile();
		fitnessedotJar.deleteOnExit();
		
		Assert.assertEquals(Kind.WARNING, 
			descriptor.doCheckFitnessePathToJar(tmpFile.getParentFile().getAbsolutePath()).kind);
	}
	
	@Test
	public void emptyPathToFitnesseRootShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
				descriptor.doCheckFitnessePathToRoot("").kind);
	}
	
	@Test
	public void incorrectlyEndedFitnesseRootShouldBeWarning() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-root", "");
		File root = new File(tmpFile.getParentFile(), "FitNesseRoot");
		root.createNewFile();
		root.deleteOnExit();
		
		Assert.assertEquals(Kind.WARNING, 
				descriptor.doCheckFitnessePathToRoot(tmpFile.getParentFile().getAbsolutePath()).kind);
	}
	
	@Test
	public void emptyTargetPageShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnesseTargetPage("").kind);
	}
	
	@Test
	public void emptyPathToFitnesseResultsShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR, 
			descriptor.doCheckFitnessePathToXmlResultsOut("").kind);
	}
	
	public void nonExistentFitnesseResultsShouldBeOK() throws Exception {
		Assert.assertEquals(Kind.OK, 
			descriptor.doCheckFitnessePathToXmlResultsOut("aldhfashf.xml").kind);
	}
	
	@Test
	public void incorrectlyEndedFitnesseResultsShouldBeWarning() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-results", "");
		Assert.assertEquals(Kind.WARNING, 
			descriptor.doCheckFitnessePathToXmlResultsOut(tmpFile.getAbsolutePath()).kind);
	}
	
	@Test
	public void correctlyEndedFitnesseResultsShouldBeOk() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-results", "xml");
		Assert.assertEquals(Kind.OK, 
				descriptor.doCheckFitnessePathToXmlResultsOut(tmpFile.getAbsolutePath()).kind);
	}

	@Test
	public void emptyJavaWorkingDirectoryShouldBeOk() throws Exception {
		Assert.assertEquals(Kind.OK, 
				descriptor.doCheckFitnesseJavaWorkingDirectory("").kind);
	}

	@Test
	public void nonExistentJavaWorkingDirectoryShouldBeError() throws Exception {
		String noSuchDirectory = "noSuchDirectory" + System.currentTimeMillis();
		Assert.assertEquals(Kind.ERROR, 
				descriptor.doCheckFitnesseJavaWorkingDirectory(noSuchDirectory).kind);
	}

	@Test
	public void existentJavaWorkingDirectoryShouldBeOk() throws Exception {
		File tmpFile = File.createTempFile("fitnesse-results", "xml");
		Assert.assertEquals(Kind.OK, 
				descriptor.doCheckFitnesseJavaWorkingDirectory(tmpFile.getParentFile().getAbsolutePath()).kind);
	}
	
	@Test
	public void emptyHttpTimeoutShouldBeOk() throws Exception {
		Assert.assertEquals(Kind.OK,
				descriptor.doCheckFitnesseHttpTimeout("").kind);
	}

	@Test
	public void nonNumericHttpTimeoutShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR,
				descriptor.doCheckFitnesseHttpTimeout("a").kind);
	}

	@Test
	public void negativeHttpTimeoutShouldBeError() throws Exception {
		Assert.assertEquals(Kind.ERROR,
				descriptor.doCheckFitnesseHttpTimeout("-1").kind);
	}

	@Test
	public void positiveHttpTimeoutShouldBeOk() throws Exception {
		Assert.assertEquals(Kind.OK,
				descriptor.doCheckFitnesseHttpTimeout("1").kind);
	}
}
