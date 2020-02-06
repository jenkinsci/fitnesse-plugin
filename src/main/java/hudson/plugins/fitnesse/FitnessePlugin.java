package hudson.plugins.fitnesse;

import hudson.Plugin;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.*;

public class FitnessePlugin extends Plugin {
	static Templates templates;

	@Override
	public void start() throws Exception {
		initTemplate();
	}

	private static void initTemplate() throws TransformerFactoryConfigurationError, IOException,
			TransformerConfigurationException {

		InputStream is = null;
		try {
			is = FitnessePlugin.class.getResourceAsStream("fitnesse-results.xsl");
			InputStream isDeBom = InputStreamDeBOMer.deBOM(is);

			StreamSource xslSource = new StreamSource(isDeBom);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			templates = transformerFactory.newTemplates(xslSource);
		} finally {
			if (is != null)
				is.close();
		}
	}

	public static Transformer newRawResultsTransformer() throws TransformerException {
		//In case of start method is not called by Jenkins plugin life cycle management
		if (templates == null) {
			try {
				initTemplate();
			} catch (Exception e) {
				throw new TransformerException("Can't initialize template", e);
			}
		}

		return templates.newTransformer();
	}

}
