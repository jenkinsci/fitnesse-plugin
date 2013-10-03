package hudson.plugins.fitnesse.project;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.fitnesse.FitnesseResultsRecorder;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.util.DefaultBuildChooser;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.Maven;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class FitnesseProject extends Project<FitnesseProject,FitnesseBuild> implements TopLevelItem {

	private static final Logger LOGGER = Logger.getLogger(XlTestProject.class.getName());

	@DataBoundConstructor
	public FitnesseProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class<FitnesseBuild> getBuildClass() {
        return FitnesseBuild.class;
    }

    public TopLevelItemDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension(ordinal=1000)
    public static final FitnesseProjectDescriptor DESCRIPTOR = new FitnesseProjectDescriptor();

	public static final class FitnesseProjectDescriptor extends AbstractProject.AbstractProjectDescriptor {

		public FitnesseProjectDescriptor() {
		}

		public String getDisplayName() {
			return "Build a FitNesse project";
		}

		public FitnesseProject newInstance(ItemGroup parent, String name) {
			String scmUrl = Stapler.getCurrentRequest().getParameter("_.scmurl");
			String scmBranch = Stapler.getCurrentRequest().getParameter("_.scmbranch");
			String suite = Stapler.getCurrentRequest().getParameter("_.suite");
			String environment = Stapler.getCurrentRequest().getParameter("_.environment");

			FitnesseProject fitnesseProject = new FitnesseProject(parent, name);
			try {
				fitnesseProject.setScm(scmFor(scmUrl, scmBranch));
			} catch (IOException e) {
				LOGGER.log(Level.INFO, "Error initializing SCM, creating job without it", e);
			}
			try {
				fitnesseProject.getBuildersList().add(mavenBuilderFor(suite, environment));
			} catch (Exception e) {
				LOGGER.log(Level.INFO, "Error initializing Maven, creating job without it");

			}
			fitnesseProject.getBuildersList().add(new Maven("integration-test", getMavenInstallationName(), "pom.xml",
					"fitnesse.environment=" + environment + "\nfitnesse.suite=" + suite, ""));
			fitnesseProject.getPublishersList().add(new FitnesseResultsRecorder("target/fitnesse-results.xml"));
			return fitnesseProject;
		}

		private Maven mavenBuilderFor(String suite, String environment) throws Exception {
			String properties = "";
			if (isNotBlank(environment)) {
				properties += "fitnesse.environment=" + environment;
			}
			if (isNotBlank(suite)) {
				properties += "\nfitnesse.suite=" + suite;
			}

			return new Maven("integration-test", null, "pom.xml", properties, "");
		}

		private SCM scmFor(String scmUrl, String branch) {
			if (scmUrl.startsWith("git")) {
				List<BranchSpec> branches = null;
				if (isNotBlank(branch)) {
					branches = Lists.newArrayList(new BranchSpec(branch));
				}

				return new GitSCM(null, createGitRepoList(scmUrl), branches, null, false,
						Collections.<SubmoduleConfig>emptyList(), false, false, new DefaultBuildChooser(), null, null,
						false, null, null, null, null, null, false, false, false, false, null, null, false, null, false,
						false);
			} else {
				return new SubversionSCM(scmUrl);
			}
		}

		static private List<UserRemoteConfig> createGitRepoList(String url) {
			List<UserRemoteConfig> repoList = new ArrayList<UserRemoteConfig>();
			repoList.add(new UserRemoteConfig(url, null, null));
			return repoList;
		}
	}
}
