package hudson.plugins.fitnesse;

import static java.util.Collections.emptyList;
import hudson.model.AbstractBuild;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import org.kohsuke.stapler.export.Exported;

/**
 * Represents the details of a FitNesseResults instance, i.e. the FitNesse html
 * output.
 */
public class ResultsDetails extends TestResult {
	private static final Logger log = Logger.getLogger(ResultsDetails.class
			.getName());

	private static final long serialVersionUID = 3169974791899027186L;

	private FitnesseResults parentResults;
	private String name;

	public ResultsDetails(FitnesseResults parent, String name) {
		this.parentResults = parent;
		this.name = name;
	}

	private Counts getPageCounts() {
		return parentResults.getPageCounts();
	}

	/**
	 * Required for {@link TestObject#getId}
	 */
	@Override
	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return parentResults.getDisplayName() + " Details";
	}

	@Override
	public AbstractBuild<?, ?> getOwner() {
		return parentResults.getOwner();
	}

	@Override
	public TestObject getParent() {
		return parentResults;
	}

	@Override
	public AbstractTestResultAction getTestResultAction() {
		return parentResults.getTestResultAction();
	}

	@Override
	public TestResult findCorrespondingResult(String id) {
		if (id.equals(getId()))
			return this;

		return null;
	}

	/**
	 * referenced from body.jelly
	 */
	public String getDetailsHtml() {
		this.parentResults.getName();
		// return getPageCounts().content;
		// return "<b>hello world</b>";
		StringBuffer ret = new StringBuffer();
		String fileName =  parentResults.getPageCounts().contentFile;
		File file = new File(fileName);
		ret.append("<br/><b>fileName: " + parentResults.getPageCounts().contentFile + "</b>");
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				ret.append(strLine);
				ret.append("\n");
			}
			// Close the input stream
			in.close();
		} catch (Exception e) {// Catch exception if any
			return "exception while reading file: " + fileName + "\n" + e.toString();
		}
		return ret.toString();
	}

	@Override
	public int getFailCount() {
		if (!isPassed() && !isSkipped())
			return 1;
		else
			return 0;
	}

	@Override
	public int getSkipCount() {
		if (isSkipped())
			return 1;
		else
			return 0;
	}

	@Override
	public int getPassCount() {
		return isPassed() ? 1 : 0;
	}

	/**
	 * Gets the "children" of this test result that failed
	 * 
	 * @return the children of this test result, if any, or an empty collection
	 */
	@Override
	public Collection<? extends TestResult> getFailedTests() {
		return singletonListOrEmpty(!isPassed());
	}

	/**
	 * Gets the "children" of this test result that passed
	 * 
	 * @return the children of this test result, if any, or an empty collection
	 */
	@Override
	public Collection<? extends TestResult> getPassedTests() {
		return singletonListOrEmpty(isPassed());
	}

	/**
	 * Gets the "children" of this test result that were skipped
	 * 
	 * @return the children of this test result, if any, or an empty list
	 */
	@Override
	public Collection<? extends TestResult> getSkippedTests() {
		return singletonListOrEmpty(isSkipped());
	}

	private Collection<? extends hudson.tasks.test.TestResult> singletonListOrEmpty(
			boolean f) {
		if (f)
			return Collections.singletonList(this);
		else
			return emptyList();
	}

	/**
	 * @return true if the test was not skipped and did not fail, false
	 *         otherwise.
	 */
	public boolean isPassed() {
		return getPageCounts().exceptions == 0 && getPageCounts().wrong == 0;
	}

	/**
	 * Tests whether all test cases were skipped or not. TestNG allows tests to
	 * be skipped if their dependencies fail or they are part of a group that
	 * has been configured to be skipped.
	 * 
	 * @return true if the test was not executed, false otherwise.
	 */
	@Exported(visibility = 9)
	public boolean isSkipped() {
		return getPageCounts().ignored > 0
				&& getPageCounts().ignored == getNumberOfTestCases();
	}

	/**
	 * Returns the altogether number of test cases, i.e. right, wrong,
	 * exceptions and ignored.
	 */
	private int getNumberOfTestCases() {
		return getPageCounts().ignored + getPageCounts().exceptions
				+ getPageCounts().right + getPageCounts().wrong;
	}
}
