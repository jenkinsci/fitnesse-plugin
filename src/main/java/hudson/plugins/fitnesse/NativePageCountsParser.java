package hudson.plugins.fitnesse;

import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

public class NativePageCountsParser {

	public NativePageCounts parse(InputStream inputStream) throws TransformerException {
		NativePageCounts fitnessePageCounts = new NativePageCounts();
		SAXResult intermediateResult = new SAXResult(fitnessePageCounts);
		transformRawResults(inputStream, intermediateResult);
		return fitnessePageCounts;
	}

	public void transformRawResults(InputStream inputStream, Result xslResult)
			throws TransformerException {
		Transformer transformer = FitnessePlugin.newRawResultsTransformer();
		StreamSource source = new StreamSource(inputStream);
		transformer.transform(source, xslResult);
	}
}
