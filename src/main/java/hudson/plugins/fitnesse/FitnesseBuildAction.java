package hudson.plugins.fitnesse;

import hudson.model.Action;
import hudson.model.InvisibleAction;

public class FitnesseBuildAction extends InvisibleAction implements Action {

	public static final FitnesseBuildAction NULL_ACTION = new FitnesseBuildAction(true, null, 0);
	
	private final String fitnesseHost;
	private final int fitnessePort;
	private final boolean fitnesseStarted;

	public FitnesseBuildAction(boolean fitnesseStarted, String fitnesseHost, int fitnessePort) {
		this.fitnesseStarted = fitnesseStarted;
		this.fitnesseHost = fitnesseHost;
		this.fitnessePort = fitnessePort;
	}

	public String getLinkFor(String fitnessePage) {
		return getLinkFor(fitnessePage, null);
	}
	
	public String getLinkFor(String fitnessePage, String hudsonHost) {
		if (fitnesseStarted) return fitnessePage;
		
		String host = fitnesseHost;
		if (hudsonHost != null && FitnesseBuilder._LOCALHOST.equals(fitnesseHost)) {
			host = hudsonHost;
		}
		return String.format("<a href=\"http://%s:%s/%s\">%s</a>", 
				host, fitnessePort, fitnessePage, fitnessePage);
	}

}
