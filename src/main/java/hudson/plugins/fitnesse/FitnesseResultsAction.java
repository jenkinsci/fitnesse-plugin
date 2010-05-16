package hudson.plugins.fitnesse;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;

import org.kohsuke.stapler.StaplerProxy;

public class FitnesseResultsAction extends AbstractTestResultAction<FitnesseResultsAction> implements StaplerProxy {
	private static final long serialVersionUID = 1L;
	private FitnesseResults results;

	protected FitnesseResultsAction(AbstractBuild<?, ?> owner, FitnesseResults results) {
		super(owner);
		this.results = results;
		results.setOwner(owner);
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
		return String.format("(%s, %d pages: %d wrong, %d ignored or with exceptions)", 
				getResult().getName(), getTotalCount(), getFailCount(), getSkipCount());
	}
}
