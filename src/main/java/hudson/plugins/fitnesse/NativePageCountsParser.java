package hudson.plugins.fitnesse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

public class NativePageCountsParser {

	public NativePageCounts parse(InputStream inputStream, String resultFileName, PrintStream logger, String rootDirName)
			throws TransformerException, IOException {
		NativePageCounts fitnessePageCounts = new NativePageCounts(logger, resultFileName, rootDirName);
		SAXResult intermediateResult = new SAXResult(fitnessePageCounts);
		transformRawResults(inputStream, intermediateResult);
		return fitnessePageCounts;
	}

	public void transformRawResults(InputStream inputStream, Result xslResult) throws TransformerException, IOException {
		Transformer transformer = FitnessePlugin.newRawResultsTransformer();
		StreamSource source = new StreamSource(InputStreamDeBOMer.deBOM(inputStream));
		transformer.transform(source, xslResult);
	}
}
