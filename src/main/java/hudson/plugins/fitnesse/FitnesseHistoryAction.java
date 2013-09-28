package hudson.plugins.fitnesse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

public class FitnesseHistoryAction implements StaplerProxy, Action {
	private final AbstractProject<?, ?> project;

	public FitnesseHistoryAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	private List<String> getPages(List<FitnesseResults> builds) {
		Set<String> pages = new HashSet<String>();

		for (FitnesseResults results : builds) {
			for (FitnesseResults result : results.getChildResults()) {
				pages.add(result.getName());
			}
		}

		return new ArrayList<String>(pages);
	}

	private List<FitnesseResults> getBuilds(AbstractProject<?, ?> project) {
		List<FitnesseResults> result = new ArrayList<FitnesseResults>();

		for (AbstractBuild<?, ?> build : project.getBuilds()) {
			FitnesseResultsAction action = build.getAction(FitnesseResultsAction.class);
			if (action != null) {
				// Builds are iterated in reverse-chronological order, so we append to the front of the list of results.
				result.add(0, action.getResult());
			}
		}

		return result;
	}

	public Object getTarget() {
		List<FitnesseResults> builds = getBuilds(project);
		List<String> pages = getPages(builds);

		return new FitnesseHistory(project, pages, builds);
	}

	public String getIconFileName() {
		return "/plugin/fitnesse/icons/fitnesselogo-32x32.gif";
	}

	public String getDisplayName() {
		return "FitNesse History";
	}

	public String getUrlName() {
		return "fitnesseHistory";
	}

}
