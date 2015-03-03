package hudson.plugins.fitnesse;

import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

public class FitnesseResults extends TabulatedResult implements Comparable<FitnesseResults>{
	private static final String DETAILS = "Details";

	//private static final Logger log = Logger.getLogger(FitnesseResults.class.getName());
	
	private static final long serialVersionUID = 1L;
	private transient List<FitnesseResults> failed;
	private transient List<FitnesseResults> skipped;
	private transient List<FitnesseResults> passed;
	
	private Counts pageCounts;
	private TestObject parent;
	private List<FitnesseResults> details = new ArrayList<FitnesseResults>();
	private AbstractBuild<?, ?> owner;
	
	public FitnesseResults(Counts pageCounts) {
		this.pageCounts = pageCounts;
	}
	
	FitnesseResults(NativePageCounts allCounts) {
		this(allCounts.getSummary());
		for (Counts detail : allCounts.getDetails()) {
			addChild(new FitnesseResults(detail));
		}
	}

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
	 * {@link TestObject} Required to compare builds with one another e.g. show history graph
	 */
	@Override
	public TestResult findCorrespondingResult(final String id) {
		if (id.equals(getId())) return this;
		List<FitnesseResults> match = filteredCopyOfDetails(new ResultsFilter() {
			public boolean include(FitnesseResults fitnesseResults) {
				return id.equals(fitnesseResults.getId());
			}
		});
		return (match.size() == 0 ? null : match.get(0));
	}

	public void setOwner(AbstractBuild<?, ?> build) {
		this.owner = build;
	}
	
	@Override
	public AbstractBuild<?, ?> getOwner() {
		if (owner != null) return owner;
		if (parent != null) return parent.getOwner();
		return null;
	}

	@Override
	public void setParent(TestObject parentObject) {
		this.parent = parentObject;
	}

	@Override
	public TestObject getParent() {
		return parent;
	}

	/**
	 * Required for {@link TestObject#getId}
	 */
	@Override
	@Exported(visibility=2)
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
	@Exported(visibility=2)
	public int getFailCount() {
		return pageCounts.wrong + getExceptionCount();
	}

	@Exported(visibility=2)
	public int getFailOnlyCount() {
		return pageCounts.wrong;
	}

	@Override
	@Exported(visibility=2)
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
	@Exported(visibility=2)
	public int getIgnoredCount() {
		return pageCounts.ignored;
	}

	/**
	 * referenced in summary.jelly
	 */
	@Exported(visibility=2)
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
		if (isFailedOverall()) return false;
		return getPassCount() == 0;
	}

	@Override
	public Result getBuildResult() {
		if (getFailCount() > 0) return Result.UNSTABLE;
		return null;
	}

	@Override
	@Exported(visibility=2)
	public float getDuration() {
		return pageCounts.duration / 1000.0f;
	}

	@Exported(visibility=1)
	public String getResultsDate() {
		return pageCounts.resultsDate;
	}

	public boolean isEarlierThan(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().before(other.pageCounts.resultsDateAsDate());
		} catch (ParseException e) {
			return false;
		}
	}

	public boolean isLaterThan(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().after(other.pageCounts.resultsDateAsDate());
		} catch (ParseException e) {
			return false;
		}
	}

	public long millisAfter(FitnesseResults other) {
		try {
			return pageCounts.resultsDateAsDate().getTime() - 
				other.pageCounts.resultsDateAsDate().getTime();
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
	 * {@see TestObject#getTestResultAction()}
	 * Required to prevent looking for any old AbstractTestResultAction
	 * when e.g. looking for history across multiple builds 
	 */
	@Override
	public FitnesseResultsAction getTestResultAction() {
		return getParentAction();
	}

	/**
	 * {@see TestResult#getParentAction()}
	 * Required to prevent looking for any old AbstractTestResultAction
	 * when e.g. looking for history across multiple builds 
	 */
	@Override
	public FitnesseResultsAction getParentAction() {
		FitnesseResultsAction action = getOwner().getAction(FitnesseResultsAction.class);
		return action;
	}
		
	/**
	 * {@link Comparable}
	 */
	public int compareTo(FitnesseResults other) {
		return getDisplayName().compareTo(other.getDisplayName());
	}

	@Override
	@Exported(visibility=1)
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
	@Exported(visibility=1)
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
	@Exported(visibility=1)
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
	
	private List<FitnesseResults> filteredCopyOfDetails(ResultsFilter countsFilter) {
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
	 */
	public String toHtml(FitnesseResults results) {
		FitnesseBuildAction buildAction = getOwner().getAction(FitnesseBuildAction.class);
		if (buildAction == null) {
			buildAction = FitnesseBuildAction.NULL_ACTION;
		}
		return buildAction.getLinkFor(results.getName(), Hudson.getInstance().getRootUrl());
	}

	/**
	 * referenced in body.jelly. Link is apparently relative to 
	 * This is the left column, which reads the results from file
	 */
	public String getDetailsLink() {
		if (details == null) {
			return "&nbsp;";
		}

		return String.format("<a href=\"%s/%s\">%s</a>", 
				getName(), DETAILS, "Details");
//		return String.format("<a href=\"%s/%s\">%s</a>", 
//					getUrl(), "Details", "Details");
	}
	
	/**
	 * referenced in body.jelly. 
	 * The link points to the history of the fitnesse server. Note the history may not always be available.
	 */
	public String getDetailRemoteLink() {
		FitnesseBuildAction buildAction = getOwner().getAction(FitnesseBuildAction.class);
		if (buildAction == null) {
			buildAction = FitnesseBuildAction.NULL_ACTION;
		}
		return buildAction.getLinkFor(getName() + "?pageHistory&resultDate="+getResultsDate(), null, "Details");
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
	 * Returns <code>true</code> if there are child results available.
	 * So far, only returns <code>true</code> if there is {@link #getHtmlContent()}
	 * for this result available.
	 */
	@Override
	public boolean hasChildren() {
		return hasChildResults() || hasHtmlContent();
	}

	/**
	 * Returns the children of this result. Returns both the details and the html
	 * content, if available.
	 * @return the details and html content results, or an empty Collection
	 */
	@Override
	@Exported(visibility=1)
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
	 * Returns <code>true</code> if there are children FitNesse results available
	 */
	protected boolean hasChildResults() {
		return !getChildResults().isEmpty();
	}
	
	/**
	 * Returns the children FitNesse results that were added with {@link #addChild(FitnesseResults)}
	 */
	protected List<FitnesseResults> getChildResults() {
		return details;
	}

	/**
	 * Returns <code>true</code> if this results has html content
	 * that is available via {@link #getHtmlContent()}
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
