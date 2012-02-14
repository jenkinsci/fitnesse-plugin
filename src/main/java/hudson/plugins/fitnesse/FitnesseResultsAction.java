package hudson.plugins.fitnesse;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;

import org.kohsuke.stapler.StaplerProxy;

public class FitnesseResultsAction extends AbstractTestResultAction<FitnesseResultsAction> implements StaplerProxy {
	private static final long serialVersionUID = 1L;
	private FitnesseResults results;
	private int resultId;

	protected FitnesseResultsAction(AbstractBuild<?, ?> owner, FitnesseResults results, int resultId) {
		super(owner);
		this.results = results;
		this.resultId = resultId;
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
		return "fitnesseReport." + resultId;
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
		return String.format("(%s, %d pages: %d wrong or with exceptions, %d ignored)",
				getResult().getName(), getTotalCount(), getFailCount(), getSkipCount());
	}
}
