package hudson.plugins.fitnesse;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FitnesseResults extends TestResult implements Comparable<FitnesseResults>{
	private static final long serialVersionUID = 1L;
	private transient long durationInMillis = -1;
	private transient List<FitnesseResults> failed = null;
	private transient List<FitnesseResults> skipped = null;
	private transient List<FitnesseResults> passed = null;
	
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
			addDetail(new FitnesseResults(detail));
		}
	}

	void addDetail(FitnesseResults fitnesseResults) {
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
		return owner;
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
		return pageCounts.wrong;
	}

	@Override
	public int getPassCount() {
		return pageCounts.right;
	}

	@Override
	public int getSkipCount() {
		return getIgnoredCount() + getExceptionCount();
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
	
	@Override
	public float getDuration() {
		if (durationInMillis == -1) calculateDurationInMillis();
		return durationInMillis / 1000.0f;
	}

	private void calculateDurationInMillis() {
		FitnesseResults earliest = null, latest = null;
		for (FitnesseResults detail : details) {
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
	 * {@link TestObject}
	 * Required to prevent TestResult.getParentAction looking for any old AbstractTestResultAction
	 * when e.g. looking for history across multiple builds 
	 */
	@Override
	public AbstractTestResultAction<?> getTestResultAction() {
		if (super.getTestResultAction() == null) return null;
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
					return results.getFailCount() > 0;
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
					return results.getPassCount() > 0 &&
					results.getPassCount() == results.getTotalCount();
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
					return results.getTotalCount() == 0
					|| (results.getFailCount() == 0 && results.getSkipCount() > 0);
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
	 * referenced in summary.jelly
	 */
	public String toHtml(FitnesseResults results) {
		FitnesseBuildAction buildAction = getOwner().getAction(FitnesseBuildAction.class);
		if (buildAction == null) {
			buildAction = FitnesseBuildAction.NULL_ACTION;
		}
		return buildAction.getLinkFor(results.getName());
	}

}
