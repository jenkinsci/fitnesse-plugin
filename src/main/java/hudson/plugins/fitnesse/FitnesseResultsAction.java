package hudson.plugins.fitnesse;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.model.Run;

import org.kohsuke.stapler.StaplerProxy;


public class FitnesseResultsAction extends AbstractTestResultAction<FitnesseResultsAction> implements StaplerProxy {
	private FitnesseResults results;

	protected FitnesseResultsAction(Run owner, FitnesseResults results) {
		this.results = results;
		this.results.setOwner(owner);
	}

	@Override
	public int getFailCount() {
		return results.getFailCount();
	}

	@Override
	public int getTotalCount() {
		return results.getTotalCount();
	}

	@Override
	public int getSkipCount() {
		return results.getSkipCount();
	}

	@Override
	public FitnesseResults getResult() {
		return results;
	}

	/**
	 * {@link Action}
	 */
	@Override
	public String getUrlName() {
		return "fitnesseReport";
	}

	/**
	 * {@link Action}
	 */
	@Override
	public String getDisplayName() {
		return "FitNesse Results";
	}

	/**
	 * {@link Action}
	 */
	@Override
	public String getIconFileName() {
		return "/plugin/fitnesse/icons/fitnesselogo-32x32.gif";
	}

	/**
	 * {@link StaplerProxy}
	 */
	public Object getTarget() {
		return results;
	}

	/**
	 * Referenced in summary.jelly and FitnesseProjectAction/jobMain.jelly
	 */
	public String getSummary() {
		return String.format("(%d pages: %d wrong or with exceptions, %d ignored)", getTotalCount(), getFailCount(),
				getSkipCount());
	}
}
