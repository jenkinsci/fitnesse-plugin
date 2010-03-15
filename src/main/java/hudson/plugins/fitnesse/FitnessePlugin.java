package hudson.plugins.fitnesse;

import hudson.Plugin;

import java.io.InputStream;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

public class FitnessePlugin extends Plugin {
	static Templates templates;

	@Override
	public void start() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		StreamSource xslSource = new StreamSource(getXslAsInputStream());
		templates = transformerFactory.newTemplates(xslSource);
	}
	
	public InputStream getXslAsInputStream() {
		return getClass().getResourceAsStream("fitnesse-results.xsl");
	}

	public static Transformer newRawResultsTransformer() throws TransformerException {
		return templates.newTransformer();
	}

}
