package hudson.plugins.fitnesse;

import hudson.Util;
import hudson.model.Job;
import hudson.tasks.test.AbstractTestResultAction;
import jenkins.tasks.SimpleBuildStep;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerProxy;


public class FitnesseResultsAction extends AbstractTestResultAction<FitnesseResultsAction> implements StaplerProxy, SimpleBuildStep.LastBuildAction {
	private FitnesseResults results;
	private List<Action> projectActions;

	protected FitnesseResultsAction(Run<?,?> owner, FitnesseResults results, TaskListener listener) {
		this.results = results;
		this.results.setOwner(owner);
		this.results.setTaskListener(listener);
		
		List<Action> projectActions = new ArrayList<Action>();
		projectActions.add(new FitnesseProjectAction(owner.getParent()));
		projectActions.add(new FitnesseHistoryAction(owner.getParent()));
		this.projectActions = projectActions;
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

	@Override
	public Collection<? extends Action> getProjectActions() {
		Job<?,?> job = run.getParent();
		if (!Util.filter(job.getActions(), FitnesseProjectAction.class).isEmpty()) {
			return Collections.emptySet();
		}
		List<Action> projectActions = new ArrayList<Action>();
		projectActions.add(new FitnesseProjectAction(job));
		projectActions.add(new FitnesseHistoryAction(job));
		this.projectActions = projectActions;
		return this.projectActions;
	}


}
