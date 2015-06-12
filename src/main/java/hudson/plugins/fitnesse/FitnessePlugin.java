package hudson.plugins.fitnesse;

import hudson.Plugin;

import java.io.IOException;
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
	}

	public static InputStream getXslAsInputStream() throws IOException {
        InputStream inputstream = FitnessePlugin.class.getResourceAsStream("fitnesse-results.xsl");
        if (inputstream == null) throw new IOException("Cannot get access to fitnesse-results.xsl");
        return InputStreamDeBOMer.deBOM(inputstream);
	}

	public static Transformer newRawResultsTransformer() throws TransformerException, IOException {
		if (templates != null)
			return templates.newTransformer();

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		StreamSource xslSource = new StreamSource(getXslAsInputStream());
		templates = transformerFactory.newTemplates(xslSource);
		return templates.newTransformer();
	}

}
