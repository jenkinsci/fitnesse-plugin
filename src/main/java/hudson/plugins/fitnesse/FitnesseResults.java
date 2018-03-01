package hudson.plugins.fitnesse;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class FitnesseResults extends TabulatedResult implements
		Comparable<FitnesseResults> {
	private static final String DETAILS = "Details";
	private static final String FITNESSE_HOSTNAME = "FITNESSE_HOSTNAME";
	private static final String FITNESSE_PORT = "FITNESSE_PORT";

	// private static final Logger log = Logger.getLogger(FitnesseResults.class.getName());
	private static final long serialVersionUID = 1L;
	private transient List<FitnesseResults> failed;
	private transient List<FitnesseResults> skipped;
	private transient List<FitnesseResults> passed;

	private Counts pageCounts;
	private FitnesseResults parent;
	private List<FitnesseResults> details = new ArrayList<FitnesseResults>();
	private transient Run<?,?> owner;
	private transient TaskListener listener;

	public FitnesseResults(Counts pageCounts) {
		this.pageCounts = pageCounts;
	}

	FitnesseResults(NativePageCounts allCounts) {
		this(allCounts.getSummary());
		for (Counts detail : allCounts.getDetails()) {
			addChild(new FitnesseResults(detail));
		}
	}

	@SuppressFBWarnings("SE_PRIVATE_READ_RESOLVE_NOT_INHERITED")
	private Object readResolve() {
		// for some reason, XStream does not instantiate the details list,
		// so we do it manually
		if (details == null) {
			details = new ArrayList<FitnesseResults>();
		}
		return this;
	}

	void addChild(FitnesseResults fitnesseResults) {
		details.add(fitnesseResults);
		fitnesseResults.setParent(this);
	}

	/**
	 * {@link TestObject} Required to compare builds with one another e.g. show
	 * history graph
	 */
	@Override
	public TestResult findCorrespondingResult(final String id) {
		if (id.equals(getId()))
			return this;
		List<FitnesseResults> match = filteredCopyOfDetails(new ResultsFilter() {
			public boolean include(FitnesseResults fitnesseResults) {
				return id.equals(fitnesseResults.getId());
			}
		});
		return (match.size() == 0 ? null : match.get(0));
	}

	public void setOwner(Run<?,?> build) {
		this.owner = build;
	}
	
	public void setTaskListener(TaskListener listener) {
		this.listener = listener;
	}

	@Override
	public AbstractBuild<?, ?> getOwner() {
		if (owner != null)
			return (AbstractBuild<?, ?>) owner;
		if (parent != null)
			return parent.getOwner();
		return null;
	}

	public TaskListener getTaskListener() {
		return listener;
	}
	
	@Override
	public void setParent(TestObject parentObject) {
		this.parent = (FitnesseResults) parentObject;
	}

	@Override
	public TestObject getParent() {
		return parent;
	}

	/**
	 * Required for {@link TestObject#getId}
	 */
	@Override
	@Exported(visibility = 2)
	public String getName() {
		return pageCounts.page;
	}

	/**
	 * {@link ModelObject}
	 */
	public String getDisplayName() {
		return getName();
	}

	@Override
	@Exported(visibility = 2)
	public int getFailCount() {
		return pageCounts.wrong + getExceptionCount();
	}

	@Exported(visibility = 2)
	public int getFailOnlyCount() {
		return pageCounts.wrong;
	}

	@Override
	@Exported(visibility = 2)
	public int getPassCount() {
		return pageCounts.right;
	}

	@Override
	public int getSkipCount() {
		return getIgnoredCount();
	}

	/**
	 * referenced in summary.jelly
	 */
	@Exported(visibility = 2)
	public int getIgnoredCount() {
		return pageCounts.ignored;
	}

	/**
	 * referenced in summary.jelly
	 */
	@Exported(visibility = 2)
	public int getExceptionCount() {
		return pageCounts.exceptions;
	}

	public boolean isFailedOverall() {
		return (getFailCount() > 0 || getExceptionCount() > 0);
	}

	public boolean isPassedOverall() {
		return isPassed();
	}

	@Override
	public boolean isPassed() {
		return !isFailedOverall() && !isSkippedOverall();
	}

	public boolean isSkippedOverall() {
		if (isFailedOverall())
			return false;
		return getPassCount() == 0;
	}

	@Override
	public Result getBuildResult() {
		if (getFailCount() > 0)
			return Result.UNSTABLE;
		return null;
	}

	@Override
	@Exported(visibility = 2)
	public float getDuration() {
		return pageCounts.duration / 1000.0f;
	}

	@Exported(visibility = 1)
	public String getResultsDate() {
		return pageCounts.resultsDate;
	}

	public boolean isEarlierThan(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().before(
					other.pageCounts.resultsDateAsDate());
		} catch (ParseException e) {
			return false;
		}
	}

	public boolean isLaterThan(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().after(
					other.pageCounts.resultsDateAsDate());
		} catch (ParseException e) {
			return false;
		}
	}

	public long millisAfter(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().getTime()
					- other.pageCounts.resultsDateAsDate().getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	/**
	 * referenced in body.jelly
	 */
	public String getHeadlineText() {
		return pageCounts.toString();
	}

	/**
	 * {@see TestObject#getTestResultAction()} Required to prevent looking for
	 * any old AbstractTestResultAction when e.g. looking for history across
	 * multiple builds
	 */
	@Override
	public FitnesseResultsAction getTestResultAction() {
		return getParentAction();
	}

	/**
	 * {@see TestResult#getParentAction()} Required to prevent looking for any
	 * old AbstractTestResultAction when e.g. looking for history across
	 * multiple builds
	 */
	@Override
	public FitnesseResultsAction getParentAction() {
		FitnesseResultsAction action = getOwner().getAction(
				FitnesseResultsAction.class);
		return action;
	}

	/**
	 * {@link Comparable}
	 */
	public int compareTo(FitnesseResults other) {
		return getDisplayName().compareTo(other.getDisplayName());
	}
	
	@Override
	public int hashCode() {
		return getDisplayName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FitnesseResults other = (FitnesseResults) obj;
	
		return getDisplayName().equals(other.getDisplayName());
	}

	@Override
	@Exported(visibility = 1)
	public Collection<FitnesseResults> getFailedTests() {
		if (failed == null) {
			failed = filteredCopyOfDetails(new ResultsFilter() {
				public boolean include(FitnesseResults results) {
					return results.isFailedOverall();
				}
			});
		}
		return failed;
	}

	@Override
	@Exported(visibility = 1)
	public Collection<FitnesseResults> getPassedTests() {
		if (passed == null) {
			passed = filteredCopyOfDetails(new ResultsFilter() {
				public boolean include(FitnesseResults results) {
					return results.isPassedOverall();
				}
			});
		}
		return passed;
	}

	@Override
	@Exported(visibility = 1)
	public Collection<FitnesseResults> getSkippedTests() {
		if (skipped == null) {
			skipped = filteredCopyOfDetails(new ResultsFilter() {
				public boolean include(FitnesseResults results) {
					return results.isSkippedOverall();
				}
			});
		}
		return skipped;
	}

	private List<FitnesseResults> filteredCopyOfDetails(
			ResultsFilter countsFilter) {
		List<FitnesseResults> filteredCopy = new ArrayList<FitnesseResults>();
		for (FitnesseResults result : details) {
			if (countsFilter.include(result)) {
				filteredCopy.add(result);
			}
		}
		Collections.sort(filteredCopy);
		return filteredCopy;
	}

	interface ResultsFilter {
		public boolean include(FitnesseResults fitnesseResults);
	}

	/**
	 * referenced in body.jelly
	 * throws InterruptedException 
	 * throws IOException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String toHtml(FitnesseResults results) throws IOException, InterruptedException {
		FitnesseBuildAction buildAction = getFitnesseBuildAction();
		return buildAction.getLinkFor(results.getName(), Jenkins.getActiveInstance().getRootUrl());
	}

	/**
	 * referenced in body.jelly. Link is apparently relative to This is the left
	 * column, which reads the results from file
	 */
	public String getDetailsLink() {
		if (details == null) {
			return "&nbsp;";
		}

		return String.format("<a href=\"%s/%s\">%s</a>", getName(), DETAILS,
				"Details");
	}

	/**
	 * referenced in body.jelly. The link points to the history of the fitnesse
	 * server. Note the history may not always be available.
	 * throws InterruptedException 
	 * throws IOException 
	 */
	public String getDetailRemoteLink() throws IOException, InterruptedException {
		FitnesseBuildAction buildAction = getFitnesseBuildAction();
		return buildAction.getLinkFor(getName() + "?pageHistory&resultDate="
				+ getResultsDate(), null, "Details");
	}

	public String getRunTestRemoteLink() throws IOException, InterruptedException {
		FitnesseBuildAction buildAction = getFitnesseBuildAction();
		String image = "<img class=\"icon-next icon-md\" title=\"Run Test\" src=\"/static/abafcc7b/images/24x24/next.png\" />";
		return buildAction.getLinkFor(getName() + "?test", null, image);
	}

	private FitnesseBuildAction getFitnesseBuildAction() throws IOException, InterruptedException {
		FitnesseBuildAction buildAction = getOwner().getAction(FitnesseBuildAction.class);
		if (buildAction == null) {
			buildAction = getDefaultFitnesseBuildAction();
		}
		return buildAction;
	}

	private FitnesseBuildAction getDefaultFitnesseBuildAction() throws IOException, InterruptedException {
		final FitnesseBuildAction buildAction;Map<String, String> envVars =  getOwner().getEnvironment(listener);
		if (envVars.containsKey(FITNESSE_HOSTNAME) && envVars.containsKey(FITNESSE_PORT)) {
            buildAction = new FitnesseBuildAction(false, envVars.get(FITNESSE_HOSTNAME), Integer.parseInt(envVars.get(FITNESSE_PORT)));
        } else {
            buildAction = FitnesseBuildAction.NULL_ACTION;
        }
		return buildAction;
	}

	/**
	 * called from links embedded in history/trend graphs
	 * TODO: Expose sub-suites as separate elements of the fitnesse report.
	 */
	@Override
	public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
		TestResult result = findCorrespondingResult(token);
		if (result != null) {
			return result;
		}
		return findChildByName(token);
	}

	@SuppressWarnings("unchecked")
	private <T extends TestResult> T findChildByName(String aName) {
		Collection<? extends TestResult> children = getChildren();
		for (TestResult child : children) {
			if (aName.equals(child.getName())) {
				return (T) child;
			}
		}

		return null;
	}

	/**
	 * Returns <code>true</code> if there are child results available. So far,
	 * only returns <code>true</code> if there is {@link #getHtmlContent()} for
	 * this result available.
	 */
	@Override
	public boolean hasChildren() {
		return hasChildResults() || hasHtmlContent();
	}

	/**
	 * Returns the children of this result. Returns both the details and the
	 * html content, if available.
	 *
	 * @return the details and html content results, or an empty Collection
	 */
	@Override
	@Exported(visibility = 1)
	public Collection<? extends TestResult> getChildren() {
		if (!hasChildren()) {
			return Collections.emptyList();
		}
		List<TestResult> children = new ArrayList<TestResult>(getChildResults());
		if (hasHtmlContent()) {
			ResultsDetails htmlContent = new ResultsDetails(this, DETAILS);
			children.add(htmlContent);
		}
		return children;
	}

	/**
	 * Returns <code>true</code> if there are children FitNesse results
	 * available
	 */
	protected boolean hasChildResults() {
		return !getChildResults().isEmpty();
	}

	/**
	 * Returns the children FitNesse results that were added with
	 * {@link #addChild(FitnesseResults)}
	 */
	protected List<FitnesseResults> getChildResults() {
		return details;
	}

	/**
	 * Returns <code>true</code> if this results has html content that is
	 * available via {@link #getHtmlContent()}
	 */
	protected boolean hasHtmlContent() {
		return pageCounts != null && pageCounts.contentFile != null;
	}

	/**
	 * Make page counts accessible to ResultsDetails
	 */
	Counts getPageCounts() {
		return pageCounts;
	}
}
