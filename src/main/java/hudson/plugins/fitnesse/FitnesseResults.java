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
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class FitnesseResults extends TabulatedResult implements Comparable<FitnesseResults>{
	private static final Logger log = Logger.getLogger("FitNesse");
	
	private static final long serialVersionUID = 1L;
	private transient boolean durationCalculated;
	private transient long durationInMillis;
	private transient List<FitnesseResults> failed = null;
	private transient List<FitnesseResults> skipped = null;
	private transient List<FitnesseResults> passed = null;
	
	private Counts pageCounts;
	private TestObject parent;
	private List<FitnesseResults> childResults = new ArrayList<FitnesseResults>();
	private AbstractBuild<?, ?> owner;
	
	public FitnesseResults(Counts pageCounts) {
		this.pageCounts = pageCounts;
	}
	
	FitnesseResults(NativePageCounts allCounts) {
		this(allCounts.getSummary());
		for (Counts detail : allCounts.getDetails()) {
			addDetail(new FitnesseResults(detail));
		}
	}

	void addDetail(FitnesseResults fitnesseResults) {
		childResults.add(fitnesseResults);
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
	public int getFailCount() {
		return pageCounts.wrong + getExceptionCount();
	}

	public int getFailOnlyCount() {
		return pageCounts.wrong;
	}

	@Override
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
	public int getIgnoredCount() {
		return pageCounts.ignored;
	}

	/**
	 * referenced in summary.jelly
	 */
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
		if (getExceptionCount() > 0) return true;
		return getPassCount() == 0;
	}

	@Override
	public Result getBuildResult() {
		if (getFailCount() > 0) return Result.FAILURE;
		return null;
	}

	@Override
	public float getDuration() {
		if (!durationCalculated) calculateDurationInMillis();
		return durationInMillis / 1000.0f;
	}

	private void calculateDurationInMillis() {
		FitnesseResults earliest = null, latest = null;
		for (FitnesseResults detail : childResults) {
			if (earliest == null) {
				earliest = detail;
			} else if (detail.isEarlierThan(earliest)) {
				earliest = detail;
			}
			if (latest == null) {
				latest = detail;
			} else if (detail.isLaterThan(latest)) {
				latest = detail;
			}
		}
		durationInMillis = latest.millisAfter(earliest);
		durationCalculated = true;
	}
	
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
		for (FitnesseResults result : childResults) {
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
	 * referenced in body.jelly
	 */
	public String getDetailsLink(FitnesseResults results) {
		if (results.childResults == null) {
			return "&nbsp;";
		}

		String page = results.getName() + "Details";
		return String.format("<a href=\"%s\">%s</a>", 
					page, "Details");
	}

	@Override
	public String getErrorDetails() {
		if (pageCounts.content != null) {
			return pageCounts.content;
		}
		return "";
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

	private <T extends TestResult> T findChildByName(String aName) {
		Collection<? extends TestResult> children = getChildren();
		for (TestResult child : children) {
			if (aName.equals(child.getName())) {
				return (T) child;
			}
		}
		
		return null;
	}
	
	@Override
	public String getUrl() {
		String url = super.getUrl();
		return url;
	}
	
	@Override
	public Collection<? extends TestResult> getChildren() {
		if (!hasChildren()) {
			return Collections.emptyList();
		}
		
		List<TestResult> children = new ArrayList<TestResult>(childResults.size());
		for (FitnesseResults child : childResults) {
			TestResult childResult = getChildResult(child);
			if (childResult != null) {
				children.add(childResult);
			}
		}

		return children;
	}

	protected TestResult getChildResult(FitnesseResults child) {
		if (!child.hasResultsDetails()) {
			return null;
		}
		return new ResultsDetails(this, child.getName() + "Details", child.pageCounts);
	}

	@Override
	public boolean hasChildren() {
		for (FitnesseResults child : childResults) {
			if (child.hasResultsDetails()) {
				return true;
			}
		}
		return false;
	}

	protected List<FitnesseResults> getChildResults() {
		return childResults;
	}
	
	protected boolean hasResultsDetails() {
		return pageCounts != null && pageCounts.content != null;
	}
	
	
	
	/**
	 * Returns the html details of this result.
	 */
	protected String getResultsDetails() {
		if (hasResultsDetails()) {
			return pageCounts.content;
		}
		return "";
	}
	
	@Override
	public String getStdout() {
		return getResultsDetails();
	}
}
