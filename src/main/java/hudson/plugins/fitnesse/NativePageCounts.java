package hudson.plugins.fitnesse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class NativePageCounts extends DefaultHandler {
	public static final String PAGE = "page";
	public static final String PSEUDO_PAGE = "name";
	public static final String CONTENT = "content";
	public static final String APPROX_RESULT_DATE = "approxResultDate";
	public static final String RIGHT = "right";
	public static final String WRONG = "wrong";
	public static final String IGNORED = "ignored";
	public static final String EXCEPTIONS = "exceptions";
	public static final String DURATION = "duration";
	public static final String SUMMARY = "summary";
	public static final String DETAIL = "detail";
	private static final List<String> COUNTABLE = Arrays.asList(new String[] { SUMMARY, DETAIL });

	private Counts summary;
	private final Map<String, Counts> allCounts = new HashMap<String, Counts>();

	private final String rootDirName;
	private final PrintStream logger;
	private final String resultFileName;

	public NativePageCounts(PrintStream logger, String resultFileName, String rootDirName) {
		this.logger = logger;
		this.rootDirName = rootDirName;
		this.resultFileName = resultFileName;
		logger.println("Write fitnesse results to: " + rootDirName);
	}

	/**
	 * Stores the actual fitnesse results. We do not want to merge them into
	 * build.xml since the results may have size of several MB. E.g. in a suite
	 * with lots of fitnesse-tests the user usually wants to see a view results.
	 * Putting all fitnesse results into build.xml will load all results into
	 * memory which can slow down the system.
	 * 
	 * The allContents is read later on. The content is written to separate
	 * files.
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {

		if (COUNTABLE.contains(qName)) {
			String targetPage;
			if (qName.equals(SUMMARY)) {
				targetPage = resultFileName;
			} else {
				String page = attributes.getValue(PAGE);
				String pseudoPage = attributes.getValue(PSEUDO_PAGE);
				targetPage = page == null || page.equals("") ? pseudoPage : page;
			}

			String rightStr = attributes.getValue(RIGHT);
			String wrongStr = attributes.getValue(WRONG);
			String ignoredStr = attributes.getValue(IGNORED);
			String exceptionsStr = attributes.getValue(EXCEPTIONS);
			String durationStr = attributes.getValue(DURATION);
			int right = Integer.parseInt(rightStr);
			int wrong = Integer.parseInt(wrongStr);
			int ignored = Integer.parseInt(ignoredStr);
			int exceptions = Integer.parseInt(exceptionsStr);
			int duration = StringUtils.isEmpty(durationStr) ? 0 : Integer.parseInt(durationStr); //to manage previous version of FitNesse
			String resultsDate = qName.equals(SUMMARY) ? "" : resultsDateOf(attributes.getValue(APPROX_RESULT_DATE));

			String contentFileName = writeFitnesseResultFiles(targetPage, attributes.getValue(CONTENT));

			Counts counts = new Counts(targetPage, resultsDate, right, wrong, ignored, exceptions, duration, contentFileName);
			allCounts.put(counts.page, counts);

			if (qName.equals(SUMMARY)) {
				summary = counts;
			}
		}
	}

	public String resultsDateOf(String approxResultDate) {
		int pos = approxResultDate.indexOf('&');
		if (pos == -1)
			return approxResultDate;
		return approxResultDate.substring(0, pos);
	}

	public int size() {
		return allCounts.size();
	}

	public Counts getSummary() {
		if (summary != null && summary.right == 0 && summary.wrong == 0 && summary.ignored == 0 && summary.exceptions == 0) {
			List<Counts> details = getDetails();
			if (details.size() == 1) {
				return details.get(0);
			}
		}
		return summary;
	}

	public Collection<Counts> getAllCounts() {
		return allCounts.values();
	}

	public List<Counts> getDetails() {
		ArrayList<Counts> details = new ArrayList<Counts>();
		for (String key : allCounts.keySet()) {
			Counts counts = allCounts.get(key);
			if (counts != summary)
				details.add(counts);
		}
		return details;
	}

	static final class Counts {
		static final SimpleDateFormat RESULTS_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

		public final String page;
		public final String resultsDate;
		public final int right;
		public final int wrong;
		public final int ignored;
		public final int exceptions;
		public final int duration;

		public String content; // not used, keep for backward compatibility

		// stores the file-path where to find the actual fitnesse result (html)
		public final String contentFile;

		public Counts(String page, String resultsDate, int right, int wrong, int ignored, int exceptions, int duration,
				String contentFile) {
			this.page = page;
			this.resultsDate = resultsDate;
			this.right = right;
			this.wrong = wrong;
			this.ignored = ignored;
			this.exceptions = exceptions;
			this.duration = duration;
			this.contentFile = contentFile;
		}

		public Date resultsDateAsDate() throws ParseException {
			return RESULTS_DATE_FORMAT.parse(resultsDate);
		}

		@Override
		public String toString() {
			return String.format("%s (%s): %s right, %s wrong, %s ignored, %s exceptions, in %s ms", page, resultsDate,
					right, wrong, ignored, exceptions, duration);
		}

	}

	/**
	 * Gets a parsed fitnesse result and writes it to separate file. Putting the
	 * fitnesse result in a separate file as performance reasons. E.g. for a huge
	 * Test-Suite the actual fitnesse result can grow up to several MB. First
	 * implementation of fitnesse plugin has stored the result to the build.xml.
	 * This was very handy to present the result but slowed down jenkins since a
	 * request to the fitnesse result leat to putting the entire build.xml into
	 * the memory. With this function the fitnesse result is only load to memory
	 * if the user clicks on it.
	 */
	private String writeFitnesseResultFiles(String pageName, String htmlContent) {
		if (null == htmlContent) {
			logger.println(" Could not find content for page: " + pageName);
			return null;
		}
		BufferedWriter out = null;
		String fileName = rootDirName + pageName;
		try {
			// Create separate file for every test in a suite
			FileWriter fstream = new FileWriter(fileName);
			out = new BufferedWriter(fstream);
			out.write(htmlContent);
			logger.println(" File: " + fileName + " wrote");
			return fileName;
		} catch (IOException e) {
			logger.println("Error while writing to out file: " + fileName + "\n" + e.toString());
		} finally {
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
					logger.println("Could not close out stream: " + fileName + "\n" + e.toString());
				}
			}
		}
		return null;
	}
}
