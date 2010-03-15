package hudson.plugins.fitnesse;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class FitnessePluginTest {
	
	private FitnessePlugin plugin;

	public FitnessePluginTest() throws Exception {
		plugin = new FitnessePlugin();
		plugin.start();
	}

	@Test
	public void templatesShouldGenerateTransformers() throws Exception {
		Assert.assertNotNull(FitnessePlugin.templates);
		Assert.assertNotNull(FitnessePlugin.newRawResultsTransformer());
	}
	
	@Test
	public void pluginShouldFindXslThroughClassLoader() throws Exception {
		InputStream xslAsInputStream = plugin.getXslAsInputStream();
		try {
			Assert.assertNotNull(xslAsInputStream);
			xslAsInputStream.close();
		} catch (IOException e) {
			//swallow
		}
	}

}
