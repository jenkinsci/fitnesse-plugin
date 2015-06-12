package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

public class NativePageCountsTest {

	@Test
	public void countsShouldCollectValuesFromConstructor() {
		String contentFileName = "./fileName";
		Counts actual = new Counts("name", "201003112307", 1, 2, 3, 4, 5, contentFileName);
		Assert.assertEquals("name", actual.page);
		Assert.assertEquals(1, actual.right);
		Assert.assertEquals(2, actual.wrong);
		Assert.assertEquals(3, actual.ignored);
		Assert.assertEquals(4, actual.exceptions);
		Assert.assertEquals(5, actual.duration);
		Assert.assertEquals(contentFileName, actual.contentFile);
	}

	@Test
	public void countsToStringShouldSpellOutValues() {
		Counts actual = new Counts("name", "2010xxxx", 11, 10, 9, 8, 7, null);
		Assert.assertEquals("name (2010xxxx): 11 right, 10 wrong, 9 ignored, 8 exceptions, in 7 ms", actual.toString());
	}

	@Test
	public void resultsDateShouldParseStringToDate() throws ParseException {
		Calendar calendar = Calendar.getInstance();
		calendar.clear(Calendar.MILLISECOND);
		Date aDate = calendar.getTime();
		Counts actual = new Counts("name", Counts.RESULTS_DATE_FORMAT.format(aDate), 11, 10, 9, 8, 7, null);
		Assert.assertEquals(aDate, actual.resultsDateAsDate());
	}

	@Test
	public void resultsDateOfShouldStripAnyTrailingGooFromApproxDate() {
		NativePageCounts results = new NativePageCounts(System.out, "testResult.xml", "./target/");
		Assert.assertEquals("abc", results.resultsDateOf("abc&amp;"));
		Assert.assertEquals("abc", results.resultsDateOf("abc"));
	}

	@Test
	public void resultsShouldCollectSummaryFromAttributes() {
		NativePageCounts results = new NativePageCounts(System.out, "testResult.xml", "./target/");
		AttributesImpl attributes = new AttributesImpl();
		addSummaryAttributes(attributes, "1", "2", "3", "4", "5");
		results.startElement("", "", NativePageCounts.SUMMARY, attributes);
		Assert.assertEquals(1, results.size());
		Assert.assertNotNull(results.getSummary());
		Assert.assertEquals(0, results.getDetails().size());
		Assert.assertEquals("testResult.xml", results.getSummary().page);
		Assert.assertEquals("", results.getSummary().resultsDate);
		Assert.assertEquals(1, results.getSummary().right);
		Assert.assertEquals(2, results.getSummary().wrong);
		Assert.assertEquals(3, results.getSummary().ignored);
		Assert.assertEquals(4, results.getSummary().exceptions);
		Assert.assertEquals(5, results.getSummary().duration);
	}

	private void addSummaryAttributes(AttributesImpl attributes, String right, String wrong, String ignored,
			String exceptions, String duration) {
		attributes.addAttribute("", "", NativePageCounts.PSEUDO_PAGE, "String", "pseudo-name");
		attributes.addAttribute("", "", NativePageCounts.APPROX_RESULT_DATE, "String", "20100311210804&amp;format=xml");
		attributes.addAttribute("", "", NativePageCounts.RIGHT, "String", right);
		attributes.addAttribute("", "", NativePageCounts.WRONG, "String", wrong);
		attributes.addAttribute("", "", NativePageCounts.IGNORED, "String", ignored);
		attributes.addAttribute("", "", NativePageCounts.EXCEPTIONS, "String", exceptions);
		attributes.addAttribute("", "", NativePageCounts.DURATION, "String", duration);
	}

	@Test
	public void resultsShouldCollectDetailFromAttributes() {
		NativePageCounts results = new NativePageCounts(System.out, "testResult.xml", "./target/");
		AttributesImpl attributes = new AttributesImpl();
		attributes.addAttribute("", "", NativePageCounts.PAGE, "String", "name");
		String resultsDate = "20100311210804";
		addDetailAttributes(attributes, resultsDate);
		results.startElement("", "", NativePageCounts.DETAIL, attributes);
		Assert.assertEquals(1, results.size());
		Assert.assertNull(results.getSummary());
		Assert.assertEquals(1, results.getDetails().size());
		Assert.assertEquals("name", results.getDetails().get(0).page);
		Assert.assertEquals(resultsDate, results.getDetails().get(0).resultsDate);
		Assert.assertEquals(5, results.getDetails().get(0).right);
		Assert.assertEquals(6, results.getDetails().get(0).wrong);
		Assert.assertEquals(7, results.getDetails().get(0).ignored);
		Assert.assertEquals(8, results.getDetails().get(0).exceptions);
		Assert.assertEquals(9, results.getDetails().get(0).duration);
		Assert.assertEquals("./target/name", results.getDetails().get(0).contentFile);
	}

	private void addDetailAttributes(AttributesImpl attributes, String resultsDate) {
		attributes.addAttribute("", "", NativePageCounts.APPROX_RESULT_DATE, "String", resultsDate);
		attributes.addAttribute("", "", NativePageCounts.RIGHT, "String", "5");
		attributes.addAttribute("", "", NativePageCounts.WRONG, "String", "6");
		attributes.addAttribute("", "", NativePageCounts.IGNORED, "String", "7");
		attributes.addAttribute("", "", NativePageCounts.EXCEPTIONS, "String", "8");
		attributes.addAttribute("", "", NativePageCounts.DURATION, "String", "9");
		attributes.addAttribute("", "", NativePageCounts.CONTENT, "String", "<tr></tr>");
	}

	@Test
	public void resultsOfTestShouldCollectSummaryFromDetail() {
		NativePageCounts results = new NativePageCounts(System.out, "testResult.xml", "./target/");
		AttributesImpl attributes = new AttributesImpl();
		attributes.addAttribute("", "", NativePageCounts.PAGE, "String", "name");
		String resultsDate = "20100311210804";
		addDetailAttributes(attributes, resultsDate);
		results.startElement("", "", NativePageCounts.DETAIL, attributes);
		attributes = new AttributesImpl();
		// Single test has zero-ised summary
		addSummaryAttributes(attributes, "0", "0", "0", "0", "0");
		results.startElement("", "", NativePageCounts.SUMMARY, attributes);
		Assert.assertEquals(2, results.size());
		Assert.assertEquals(1, results.getDetails().size());
		Assert.assertSame(results.getSummary(), results.getDetails().get(0));
	}
}
