package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.DescribableList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FitnesseBuilderTest {
	@Test
	public void getJdkShouldReturnSpecificJavaHomeIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		final String expectedJavaHome = "jdk1.6.0_18";
		options.put(FitnesseBuilder.FITNESSE_JDK, expectedJavaHome);
		Assert.assertEquals(expectedJavaHome, builder.getFitnesseJdk());
	}

	@Test
	public void getJdkShouldReturnNothingIfNotSpecifiedSoThatTheDefaultJDKIsUsed() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		String expectedJavaHome = "";
		Assert.assertEquals(expectedJavaHome, builder.getFitnesseJdk());
	}

	@Test
	public void getPortShouldReturnLocalPortIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "99");
		Assert.assertEquals("99", builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, null);
		Assert.assertEquals("99", builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "");
		Assert.assertEquals("99", builder.getFitnessePort());
	}


	@Test
	public void getPortShouldReturnRemotePortIfSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "999");
		Assert.assertEquals("999", builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, null);
		Assert.assertEquals("999", builder.getFitnessePort());

		options.put(FitnesseBuilder.FITNESSE_PORT_LOCAL, "");
		Assert.assertEquals("999", builder.getFitnessePort());
	}


    @Test
    public void getSslShouldReturnSslIfSpecified() {
        final HashMap<String, String> options = new HashMap<String, String>();
        final FitnesseBuilder builder = new FitnesseBuilder(options);

        options.put(FitnesseBuilder.FITNESSE_ENABLE_SSL, "True");
        Assert.assertEquals(true, builder.getFitnesseSsl());

        options.put(FitnesseBuilder.FITNESSE_ENABLE_SSL, null);
        Assert.assertEquals(false, builder.getFitnesseSsl());

        options.put(FitnesseBuilder.FITNESSE_ENABLE_SSL, "");
        Assert.assertEquals(false, builder.getFitnesseSsl());
    }

	@Test
	public void getPortShouldReturnEnvValueIfMacroIsSpecified() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		EnvVars envVars = new EnvVars();
		envVars.put("PORT", "99");
		options.put(FitnesseBuilder.FITNESSE_PORT_REMOTE, "$PORT");

		Assert.assertEquals(99, builder.getFitnessePort(envVars));
	}

	@Test
	public void getHostShouldReturnLocalHostIfStartBuildIsTrue() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		options.put(FitnesseBuilder.START_FITNESSE, "True");
		Assert.assertTrue(builder.getFitnesseStart());
		Assert.assertEquals("localhost", builder.getFitnesseHost());

		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("localhost", builder.getFitnesseHost());
	}

	@Test
	public void getHostShouldReturnSpecifiedHostIfStartBuildIsFalse() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		options.put(FitnesseBuilder.START_FITNESSE, "False");
		options.put(FitnesseBuilder.FITNESSE_HOST, "hudson.local");
		Assert.assertFalse(builder.getFitnesseStart());
		Assert.assertEquals("hudson.local", builder.getFitnesseHost());

		options.put(FitnesseBuilder.FITNESSE_HOST, "abracadabra");
		Assert.assertEquals("abracadabra", builder.getFitnesseHost());
	}

	@Test
	public void getHttpTimeoutShouldReturn60000UnlessValueIsExplicit() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		Assert.assertEquals("60000", builder.getFitnesseHttpTimeout());

		options.put(FitnesseBuilder.HTTP_TIMEOUT, "1000");
		Assert.assertEquals("1000", builder.getFitnesseHttpTimeout());
	}

	@Test
	public void getTestTimeoutShouldReturn60000UnlessValueIsExplicit() {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		Assert.assertEquals(60000, builder.getFitnesseTestTimeout());
		options.put(FitnesseBuilder.TEST_TIMEOUT, "1000");
		Assert.assertEquals(1000, builder.getFitnesseTestTimeout());
	}

	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarUnlessValueIsExplicit() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		File tmpFile = File.createTempFile("fitnesse", ".jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getAbsolutePath());

		Assert.assertEquals(tmpFile.getParentFile().getAbsolutePath(), builder.getFitnesseJavaWorkingDirectory());

		options.put(FitnesseBuilder.JAVA_WORKING_DIRECTORY, "/some/explicit/path");
		Assert.assertEquals("/some/explicit/path", builder.getFitnesseJavaWorkingDirectory());
	}

	@Test
	public void getJavaWorkingDirShouldReturnParentOfFitnessseJarEvenIfRelativeToBuildDir() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		File tmpFile = new File("relativePath", "fitnesse.jar");
		options.put(FitnesseBuilder.PATH_TO_JAR, tmpFile.getPath());

		Assert.assertEquals("relativePath", builder.getFitnesseJavaWorkingDirectory());
	}

	@Test
	public void getJavaWorkingDirShouldBeEmptyIfFitnessseJarUnspecified() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		FitnesseBuilder builder = new FitnesseBuilder(options);

		Assert.assertEquals("", builder.getFitnesseJavaWorkingDirectory());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void getFitnesseHostShouldNotThrowANullPointerWhenNodePropertyIsNull() throws InterruptedException,
			IOException {

		AbstractBuild build = Mockito.mock(AbstractBuild.class);
		Node node = Mockito.mock(Node.class);
		when(build.getBuiltOn()).thenReturn(node);

		DescribableList describableList = Mockito.mock(DescribableList.class);
		when(node.getNodeProperties()).thenReturn(describableList);
		when(describableList.get(EnvironmentVariablesNodeProperty.class)).thenReturn(null);

		HashMap<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, Boolean.toString(true));
		FitnesseBuilder builder = new FitnesseBuilder(options);
		EnvVars envVars = new EnvVars();
		assertEquals(FitnesseBuilder._LOCALHOST, builder.getFitnesseHost(build, envVars));
	}
}
