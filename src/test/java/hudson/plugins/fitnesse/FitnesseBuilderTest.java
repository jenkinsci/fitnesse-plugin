package hudson.plugins.fitnesse;

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
}
