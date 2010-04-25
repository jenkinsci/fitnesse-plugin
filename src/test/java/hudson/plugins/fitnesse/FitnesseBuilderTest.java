package hudson.plugins.fitnesse;

import java.io.File;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseBuilderTest {
	@Test
	public void getPortShouldReturnLocalPortIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "99");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(99, builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, null);
		Assert.assertEquals(99, builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "");
		Assert.assertEquals(99, builder.getFitnessePort());
	}
	
	@Test
	public void getPortShouldReturnRemotePortIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "999");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(999, builder.getFitnessePort());
		
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, null);
		Assert.assertEquals(999, builder.getFitnessePort());
		
		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "");
		Assert.assertEquals(999, builder.getFitnessePort());
	}
	
	@Test
	public void getHostShouldReturnLocalHostIfStartBuildIsTrue() {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, "True");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		
		Assert.assertTrue(builder.getFitnesseStart());
		Assert.assertEquals("localhost", builder.getFitnesseHost());
		
		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("localhost", builder.getFitnesseHost());
	}
	
	@Test
	public void getHostShouldReturnSpecifiedHostIfStartBuildIsFalse() {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, "False");
		options.put(FitnesseBuilder.FITNESSE_HOST, "hudson.local");
		FitnesseBuilder builder = new FitnesseBuilder(options);
		
		Assert.assertFalse(builder.getFitnesseStart());
		Assert.assertEquals("hudson.local", builder.getFitnesseHost());
		
		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("abracadabra", builder.getFitnesseHost());
	}
	
	@Test
	public void getHttpTimeoutShouldReturn60000UnlessValueIsExplicit() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(60000, builder.getFitnesseHttpTimeout());
		options.put(FitnesseBuilder.HTTP_TIMEOUT, "1000");
		Assert.assertEquals(1000, builder.getFitnesseHttpTimeout());
	}
	
	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarUnlessValueIsExplicit() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		File tmpFile = File.createTempFile("fitnesse", ".jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getAbsolutePath());
		
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals(tmpFile.getParentFile().getAbsolutePath(), 
				builder.getFitnesseJavaWorkingDirectory());
		
		options.put(FitnesseBuilder.JAVA_WORKING_DIRECTORY, "/some/explicit/path");
		Assert.assertEquals("/some/explicit/path", builder.getFitnesseJavaWorkingDirectory());
	}	
	
	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarEvenIfRelativeToBuildDir() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		File tmpFile = new File("relativePath", "fitnesse.jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getPath());
		
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals("relativePath", builder.getFitnesseJavaWorkingDirectory());
	}
	
	@Test
	public void getJavaWorkingDirShouldBeEmptyIfFitnessseJarUnspecified() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);
		Assert.assertEquals("", 
				builder.getFitnesseJavaWorkingDirectory());
	}
}
