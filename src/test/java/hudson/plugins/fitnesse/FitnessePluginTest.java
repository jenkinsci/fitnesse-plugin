package hudson.plugins.fitnesse;

import org.junit.Assert;
import org.junit.Test;

public class FitnessePluginTest {

	private FitnessePlugin plugin = new FitnessePlugin();

	public FitnessePluginTest() throws Exception {
		plugin.start();
	}

	@Test
	public void templatesShouldNotBeNull() throws Exception {
		Assert.assertNotNull(FitnessePlugin.templates);
	}

	@Test
	public void templatesShouldGenerateTransformers() throws Exception {
		Assert.assertNotNull(FitnessePlugin.newRawResultsTransformer());
	}

}
