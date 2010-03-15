package hudson.plugins.fitnesse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class NativePageCounts extends DefaultHandler {
	public static final String PAGE = "page";
	public static final String PSEUDO_PAGE = "name";
	public static final String APPROX_RESULT_DATE = "approxResultDate";
	public static final String RIGHT = "right";
	public static final String WRONG = "wrong";
	public static final String IGNORED = "ignored";
	public static final String EXCEPTIONS = "exceptions";
	public static final String SUMMARY = "summary";
	public static final String DETAIL = "detail";
	private static final List<String> COUNTABLE = Arrays.asList(new String[] {
			SUMMARY, DETAIL });

	private Counts summary;
	private Map<String, Counts> allCounts = new HashMap<String, Counts>();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (COUNTABLE.contains(qName)) {
			String page = attributes.getValue(PAGE);
			String pseudoPage = attributes.getValue(PSEUDO_PAGE);
			Counts counts = new Counts(
					page == null || page.equals("") ? pseudoPage : page,
					qName.equals(SUMMARY) ? "" : resultsDateOf(attributes.getValue(APPROX_RESULT_DATE)), 
					Integer.parseInt(attributes.getValue(RIGHT)), 
					Integer.parseInt(attributes.getValue(WRONG)), 
					Integer.parseInt(attributes.getValue(IGNORED)), 
					Integer.parseInt(attributes.getValue(EXCEPTIONS)));
			if (qName.equals(SUMMARY)) summary = counts;
			allCounts.put(counts.page, counts);
		}
	}
	
	public String resultsDateOf(String approxResultDate) {
		int pos = approxResultDate.indexOf('&');
		if (pos == -1) return approxResultDate;
		return approxResultDate.substring(0, pos);
	}

	public int size() {
		return allCounts.size();
	}

	public Counts getSummary() {
		if (summary != null && summary.right == 0 && summary.wrong == 0 
		&& summary.ignored == 0 && summary.exceptions == 0) {
			List<Counts> details = getDetails();
			if (details.size() == 1) {
				return details.get(0);
			}
		}
		return summary;
	}
	
	public List<Counts> getDetails() {
		ArrayList<Counts> details = new ArrayList<Counts>();
		for (String key : allCounts.keySet()) {
			Counts counts = allCounts.get(key);
			if (counts != summary) details.add(counts);
		}
		return details;
	}

	static final class Counts {
		private static final long serialVersionUID = 1L;
		static final SimpleDateFormat RESULTS_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
		
		public final String page;
		public final String resultsDate;
		public final int right;
		public final int wrong;
		public final int ignored;
		public final int exceptions;

		public Counts(String page, String resultsDate, int right, int wrong, int ignored, int exceptions) {
			this.page = page;
			this.resultsDate = resultsDate;
			this.right = right;
			this.wrong = wrong;
			this.ignored = ignored;
			this.exceptions = exceptions;
		}

		public Date resultsDateAsDate() throws ParseException {
			return RESULTS_DATE_FORMAT.parse(resultsDate);
		}

		@Override
		public String toString() {
			return String.format("%s (%s): %s right, %s wrong, %s ignored, %s exceptions",
							page, resultsDate, right, wrong, ignored, exceptions );
		}
		
	}
}
