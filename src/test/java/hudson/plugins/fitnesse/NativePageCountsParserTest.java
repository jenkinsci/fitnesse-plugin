package hudson.plugins.fitnesse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.transform.dom.DOMResult;

import org.junit.Assert;
import org.junit.Test;

public class NativePageCountsParserTest {
	private static final String TEST_RESULTS_HEAD = "<?xml version=\"1.0\"?><testResults>";
	private static final String PAGE_RESULTS_HEAD = "<FitNesseVersion>20100103</FitNesseVersion><rootPath>SuiteBlah</rootPath><result>";
	private static final String PAGE_RESULTS_COUNTS = "<counts><right>0</right><wrong>0</wrong><ignores>0</ignores><exceptions>0</exceptions></counts>";
	private static final String PAGE_RESULTS_NAME = "<relativePageName>TestBlah</relativePageName>";
	private static final String PAGE_RESULTS_HISTORY = "<pageHistoryLink>WikiName.SuiteBlah.SuiteAll.TestBlah?pageHistory&amp;resultDate=20100307181143&amp;format=xml</pageHistoryLink>";
	private static final String PAGE_RESULTS_TAIL = "</result>";
	private static final String TEST_RESULTS_FINAL_COUNTS = "<finalCounts><right>5</right><wrong>4</wrong><ignores>3</ignores><exceptions>2</exceptions></finalCounts>";
	private static final String TEST_RESULTS_TAIL = "</testResults>";
	private static final String RESULTS = TEST_RESULTS_HEAD
											+ PAGE_RESULTS_HEAD
											+ PAGE_RESULTS_COUNTS
											+ PAGE_RESULTS_NAME
											+ PAGE_RESULTS_HISTORY
											+ PAGE_RESULTS_TAIL
											+ TEST_RESULTS_FINAL_COUNTS
											+ TEST_RESULTS_TAIL;
	private final NativePageCountsParser fitnesseParser;
	
	public NativePageCountsParserTest() throws Exception {
		FitnessePlugin plugin = new FitnessePlugin();
		plugin.start();
		fitnesseParser = new NativePageCountsParser();
	}
	
	@Test
	public void transformRawResultsShouldProduceSomethingUsable() throws Exception {
		DOMResult domResult = new DOMResult();		
		fitnesseParser.transformRawResults(toInputStream(RESULTS), domResult);
		Assert.assertNotNull(domResult.getNode());
		Assert.assertNotNull(domResult.getNode().getFirstChild());
		Assert.assertEquals("hudson-fitnesse-plugin-report", 
				domResult.getNode().getFirstChild().getNodeName());
	}

	@Test
	public void transformRawResultsShouldIgnoreBOM() throws Exception {
		DOMResult domResult = new DOMResult();		
		fitnesseParser.transformRawResults(
				toInputStream(InputStreamDeBOMer.UTF32BE_BOM, RESULTS.getBytes()), 
				domResult);
		Assert.assertNotNull(domResult.getNode());
		Assert.assertNotNull(domResult.getNode().getFirstChild());
		Assert.assertEquals("hudson-fitnesse-plugin-report", 
				domResult.getNode().getFirstChild().getNodeName());
	}
	
	@Test
	public void parserShouldCollectFinalCounts() throws Exception {
		NativePageCounts testResults = fitnesseParser.parse(toInputStream(RESULTS));
		Assert.assertEquals(2, testResults.size());
		Assert.assertEquals("SuiteBlah", testResults.getSummary().page);
		Assert.assertEquals(5, testResults.getSummary().right);
		Assert.assertEquals(4, testResults.getSummary().wrong);
		Assert.assertEquals(3, testResults.getSummary().ignored);
		Assert.assertEquals(2, testResults.getSummary().exceptions);
	}

	@Test
	public void parserShouldCollectAllCountsFromSuiteFile() throws Exception {
		InputStream sampleXml = getClass().getResourceAsStream("fitnesse-suite-results.xml");
		NativePageCounts testResults = fitnesseParser.parse(sampleXml);
		Assert.assertEquals(15, testResults.size());
		Assert.assertEquals("SuiteBranchMain", testResults.getSummary().page);
		Assert.assertEquals(6, testResults.getSummary().right);
		Assert.assertEquals(5, testResults.getSummary().wrong);
		Assert.assertEquals(1, testResults.getSummary().ignored);
		Assert.assertEquals(2, testResults.getSummary().exceptions);
	}

	@Test
	public void parserShouldCollectAllCountsFromSingleTestFile() throws Exception {
		InputStream sampleXml = getClass().getResourceAsStream("fitnesse-test-results.xml");
		NativePageCounts testResults = fitnesseParser.parse(sampleXml);
		Assert.assertEquals(1, testResults.size());
		Assert.assertEquals("TestDecisionTable", testResults.getSummary().page);
		Assert.assertEquals(16, testResults.getSummary().right);
		Assert.assertEquals(2, testResults.getSummary().wrong);
		Assert.assertEquals(0, testResults.getSummary().ignored);
		Assert.assertEquals(0, testResults.getSummary().exceptions);
	}

	private ByteArrayInputStream toInputStream(String aString) {
		return toInputStream(aString.getBytes());
	}
	
	private ByteArrayInputStream toInputStream(byte[] bytes) {
		return toInputStream(new byte[0], bytes);
	}
	
	private ByteArrayInputStream toInputStream(byte[] prefix, byte[] bytes) {
		byte[] all = new byte[prefix.length + bytes.length];
		for (int i=0; i < prefix.length; ++i) {
			all[i] = prefix[i];
		}
		for (int i=0; i < bytes.length; ++i) {
			all[prefix.length + i] = bytes[i];
		}
		return new ByteArrayInputStream(all);
	}
	
}
