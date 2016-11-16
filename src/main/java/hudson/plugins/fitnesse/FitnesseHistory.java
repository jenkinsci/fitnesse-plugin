package hudson.plugins.fitnesse;

import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

public class FitnesseHistory {
	private Job owner;

	/** Files list */
	private Set<String> files;

	/** Pages by files */
	public final Map<String, List<String>> pages;

	/** History builds (containing details for each individual page in that build) */
	public final List<FitnesseResults> builds;


	public FitnesseHistory(Job owner, Set<String> files, Map<String, List<String>> pages,
			List<FitnesseResults> builds) {
		this.owner = owner;
		this.files = files;
		this.pages = pages;
		this.builds = builds;
	}

	@Exported(visibility = 2)
	public String getName() {
		return "FitNesse History";
	}

	public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
		return this;
	}

	public Job getOwner() {
		return owner;
	}

	public Set<String> getFiles() {
		return files;
	}

	public List<String> getPages(String file) {
		return pages.get(file);
	}

	public List<FitnesseResults> getBuilds() {
		return builds;
	}

	public String getResult(String file, String page, FitnesseResults build) {
		for (FitnesseResults childFile : build.getChildResults()) {
			if (file.equals(childFile.getName())) {
				for (FitnesseResults child : childFile.getChildResults()) {
					if (page.equals(child.getName())) {
						if (child.isPassedOverall()) {
							return "pass";
						} else if (child.isFailedOverall()) {
							return "fail";
						} else {
							return "";
						}
					}
				}
			}
		}
		return "";
	}
}
