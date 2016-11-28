package hudson.plugins.fitnesse;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.kohsuke.stapler.StaplerProxy;

import com.google.common.collect.Ordering;

public class FitnesseHistoryAction implements StaplerProxy, Action {
	private final Job<?,?> project;

	private List<FitnesseResults> builds;
	private Map<String, List<String>> allPages;
	private Set<String> allFiles;

	public FitnesseHistoryAction(Job<?, ?> project2) {
		this.project = project2;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getTarget() {
		extractValues((RunList<Run<?,?>>) project.getBuilds());
		return new FitnesseHistory(project, allFiles, allPages, builds);
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

	public void extractValues(RunList<Run<?, ?>> runList) {
		builds = new ArrayList<FitnesseResults>();
		allFiles = new HashSet<String>();
		allPages = new HashMap<String, List<String>>();

		for (Run<?,?> build : runList) {
			FitnesseResultsAction action = build.getAction(FitnesseResultsAction.class);
			if (action != null) {
				FitnesseResults result = action.getResult();

				if(!(result instanceof CompoundFitnesseResults))
				{
					FitnesseResults fakeResult = new FitnesseResults(new Counts("ALL", "", 0, 0, 0, 0, 0, "ALL"));
					fakeResult.addChild(result);
					fakeResult.setOwner(build);
					result = fakeResult;
				}
				builds.add(result);

				List<FitnesseResults> childResults = result.getChildResults();

				Set<String> files = extractfiles(childResults);
				allFiles.addAll(files);

				Map<String, List<String>> pages = extractPages(childResults);
				for (Entry<String, List<String>> entry : pages.entrySet()) {
					String newFile = entry.getKey();
					List<String> newPages = entry.getValue();
					List<String> existingPages = allPages.get(newFile);
					if (existingPages != null) {
						for (String newPage : newPages) {
							if (!existingPages.contains(newPage))
								existingPages.add(newPage);
						}
					} else {
						allPages.put(newFile, newPages);
					}
				}
			}
		}
	}

	private static Set<String> extractfiles(List<FitnesseResults> results) {
		HashSet<String> files = new HashSet<String>();
		for (FitnesseResults resultFile : results) {
			files.add(resultFile.getName());
		}
		return files;
	}

	static Map<String, List<String>> extractPages(List<FitnesseResults> results) {
		Map<String, List<String>> pages = new HashMap<String, List<String>>();

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
		return pages;
	}

	/*
	 * SORT PAGES
	 */
	private static List<String> sorted(Map<String, PageInfo> map) {
		List<PageInfo> pages = new ArrayList<PageInfo>(map.values());
		Collections.sort(pages, PageInfo.defaultOrdering());

		List<String> pagesList = new ArrayList<String>();
		for (PageInfo pageInfo : pages) {
			pagesList.add(pageInfo.page);
		}
		return pagesList;
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
