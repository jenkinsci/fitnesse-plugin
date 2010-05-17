package hudson.plugins.fitnesse;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

public class FitnesseProjectAction implements Action {

	private AbstractProject<?, ?> project;

	public FitnesseProjectAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	/**
	 * @return null to hide from left-hand list
	 */
    public String getIconFileName() {
        return null;
    }

    /**
     * @return null to hide from left-hand list
     */
    public String getDisplayName() {
        return null;
    }

    /**
     * @see Action#getUrlName()
     */
    public String getUrlName() {
        return "fitnesse";
    }
    
    /**
     * Used in floatingBox.jelly
     */
    public History getTrend() {
    	FitnesseResultsAction latestResults = getLatestResults();
    	FitnesseResults result = latestResults.getResult();
		return new History(result,500,200);
    }

	/**
	 * Used in jobMain.jelly
	 * {@see TestResultProjectAction#getLastTestResultAction()}
	 */
	public FitnesseResultsAction getLatestResults() {
        final AbstractBuild<?,?> tb = project.getLastSuccessfulBuild();
        AbstractBuild<?,?> b = project.getLastBuild();
        while(b != null) {
            FitnesseResultsAction a = b.getAction(FitnesseResultsAction.class);
            if(a != null) {
            	return a;
            } else if(b == tb) {
                // if even the last successful build didn't produce the test result,
                // that means we just don't have any tests configured.
                return null;
            }
            b = b.getPreviousBuild();
        }

        return null;
    }
}
