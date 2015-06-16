package hudson.plugins.fitnesse;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.StaplerProxy;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class FitnesseHistoryAction implements StaplerProxy, Action {
	private final AbstractProject<?, ?> project;

	private List<FitnesseResults> builds;
	private Set<String> files;
	Map<String, List<String>> pages;

	public FitnesseHistoryAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getTarget() {
		extractValues((RunList<AbstractBuild<?, ?>>) project.getBuilds());
		return new FitnesseHistory(project, files, pages, builds);
	}

	@Override
	public String getIconFileName() {
		return "/plugin/fitnesse/icons/fitnesselogo-32x32.gif";
	}

	@Override
	public String getDisplayName() {
		return "FitNesse History";
	}

	@Override
	public String getUrlName() {
		return "fitnesseHistory";
	}

	public void extractValues(List<AbstractBuild<?, ?>> projectBuilds) {
		builds = new ArrayList<FitnesseResults>();

		for (AbstractBuild<?, ?> build : projectBuilds) {
			FitnesseResultsAction action = build.getAction(FitnesseResultsAction.class);
			if (action != null) {
				FitnesseResults result = action.getResult();
				builds.add(result);

				List<FitnesseResults> childResults = result.getChildResults();
				extractfiles(childResults);
				extractPages(childResults);
			}
		}
	}

	private void extractfiles(List<FitnesseResults> results) {
		files = new HashSet<String>();
		for (FitnesseResults resultFile : results) {
			files.add(resultFile.getName());
		}
	}

	void extractPages(List<FitnesseResults> results) {
		pages = new HashMap<String, List<String>>();

		for (FitnesseResults resultFile : results) {
			Map<String, PageInfo> pagesInfo = new HashMap<String, PageInfo>();
			for (FitnesseResults result : resultFile.getChildResults()) {
				PageInfo info = pagesInfo.get(result.getName());
				if (info == null) {
					info = new PageInfo(result.getName());
					pagesInfo.put(result.getName(), info);
				}
				info.recordResult(result);
			}

			pages.put(resultFile.getName(), sorted(pagesInfo));
		}
	}

	/*
	 * SORT PAGES
	 */
	private List<String> sorted(Map<String, PageInfo> map) {
		List<PageInfo> pages = new ArrayList<PageInfo>(map.values());
		Collections.sort(pages, PageInfo.defaultOrdering());
		return Lists.transform(pages, new Function<PageInfo, String>() {

			public String apply(PageInfo input) {
				return input == null ? null : input.page;
			}
		});
	}

	private static class PageInfo {
		private final String page;

		private boolean lastResultWasPass = true;

		/** The number of switches between 'fail' and 'pass' or vice-versa. */
		private int numberOfSwitches = 0;

		/** The number of times this test was seen at all */
		private int numberOfOccurrances = 0;

		public PageInfo(String page) {
			this.page = page;
		}

		public void recordResult(FitnesseResults result) {
			if (result.isPassedOverall() || result.isFailedOverall()) {
				numberOfOccurrances++;
				if (lastResultWasPass == result.isFailedOverall()) {
					numberOfSwitches++;
				}
				lastResultWasPass = result.isPassedOverall();
			}
		}

		private Integer erraticnessIndex() {
			if (numberOfOccurrances == 0) {
				return 0;
			} else {
				return 100 * numberOfSwitches / numberOfOccurrances;
			}
		}

		public static Comparator<PageInfo> defaultOrdering() {
			return new PageInfo.ByErraticness().reverse().compound(new PageInfo.ByPage());
		}

		private static class ByErraticness extends Ordering<PageInfo> {

			public int compare(PageInfo o1, PageInfo o2) {
				return o1.erraticnessIndex().compareTo(o2.erraticnessIndex());
			}
		}

		private static class ByPage extends Ordering<PageInfo> {

			public int compare(PageInfo o1, PageInfo o2) {
				return o1.page.compareTo(o2.page);
			}
		}
	}
}
