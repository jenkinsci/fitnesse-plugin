package hudson.plugins.fitnesse;

import static java.util.Collections.emptyList;
import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
	 * referenced from body.jelly Returns Jenkins root URL to form correct paths to static resources
	 */
	public String getRootUrlFromRequest() {
		return Jenkins.getInstance().getRootUrlFromRequest();
	}

	/**
	 * referenced from body.jelly Reads the fitnesse-result from file. The
	 * result is stored on user request in order to keep the memory footprint
	 * small.
	 */
	public String getDetailsHtml() {
		StringBuffer ret = new StringBuffer();
		// get the saved filename including its path
		String fileName = parentResults.getPageCounts().contentFile;
		if (fileName == null) {
			return "error, content filename is null for page " + parentResults.getName();
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				ret.append(strLine);
			}
		} catch (IOException e) {
			return "exception while reading file: " + fileName + "\n"
					+ e.toString();
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					// think we can ignore this exception ...
				}
			}
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
