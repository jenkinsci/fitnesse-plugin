package hudson.plugins.fitnesse;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.Shell;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jvnet.hudson.test.HudsonTestCase;

public class HudsonDependentTest extends HudsonTestCase {

	public void testOwnerOfParentResultsShouldBeOwnerOfChildResults() throws Exception {
		FitnesseResults parent = new FitnesseResults((Counts)null);
		FitnesseResults child = new FitnesseResults((Counts)null);
		parent.setOwner(new FreeStyleBuild(createFreeStyleProject(getName())));
		parent.addDetail(child);
		Assert.assertSame(parent.getOwner(), child.getOwner());
	}
	
	public void testProjectStartingFitnesseWithAbsoluteAndRelativePaths() throws Exception {
		FreeStyleProject project = createFreeStyleProject(getName());
		project.getBuildersList().clear();
		project.getPublishersList().clear();
		String resultsFile = "fitnesse-results.xml";

		Map<String, String> options = new HashMap<String, String>();
		options.put(FitnesseBuilder.START_FITNESSE, Boolean.TRUE.toString());
		options.put(FitnesseBuilder.PATH_TO_JAR, 
				new File(new File(System.getProperty("user.dir")), "target/test-classes/fitnesse.jar").getAbsolutePath());
		options.put(FitnesseBuilder.PATH_TO_ROOT, 
				new File(new File(System.getProperty("user.dir")), "target/test-classes/FitNesseRoot").getAbsolutePath());
		options.put(FitnesseBuilder.FITNESSE_PORT, "8081");
		options.put(FitnesseBuilder.TARGET_PAGE, "HudsonPlugin.SuiteAll");
		options.put(FitnesseBuilder.TARGET_IS_SUITE, Boolean.TRUE.toString());
		options.put(FitnesseBuilder.PATH_TO_RESULTS, resultsFile);
		FitnesseBuilder builder = new FitnesseBuilder(options);
		
		project.getBuildersList().add(builder);
		project.getPublishersList().add(new FitnesseResultsRecorder(resultsFile));
		System.out.println("!!! AbsolutePath build !!!");
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		Assert.assertTrue(build.getLogFile().getAbsolutePath(), !Result.FAILURE.equals(build.getResult()));
		FitnesseResultsAction resultsAction = build.getAction(FitnesseResultsAction.class);
		assertExpectedResults(resultsAction);

		project.getBuildersList().clear();
		project.getPublishersList().clear();
		resultsFile = "fitnesse-results2.xml";
		
		project.getBuildersList().add(new Shell("cp " + builder.getFitnessePathToJar() + " " + build.getWorkspace().getRemote()));
		project.getBuildersList().add(new Shell("cp -r " + builder.getFitnessePathToRoot() + " " + build.getWorkspace().getRemote()));
		options.put(FitnesseBuilder.PATH_TO_JAR, "fitnesse.jar"); 
		options.put(FitnesseBuilder.PATH_TO_ROOT, "FitNesseRoot"); 
		options.put(FitnesseBuilder.PATH_TO_RESULTS, resultsFile);

		project.getBuildersList().add(builder);
		project.getPublishersList().add(new FitnesseResultsRecorder(resultsFile));
		System.out.println("!!! RelativePath build !!!");
		build = project.scheduleBuild2(0).get();
		Assert.assertTrue(build.getLogFile().getAbsolutePath(), !Result.FAILURE.equals(build.getResult()));
		resultsAction = build.getAction(FitnesseResultsAction.class);
		assertExpectedResults(resultsAction);
	}

	private void assertExpectedResults(FitnesseResultsAction resultsAction) {
		Assert.assertEquals("passed", 1, resultsAction.getResult().getPassCount());
		Assert.assertEquals("failed", 1, resultsAction.getResult().getFailCount());
		Assert.assertEquals("ignored", 1, resultsAction.getResult().getIgnoredCount());
		Assert.assertEquals("exceptions", 1, resultsAction.getResult().getExceptionCount());
	}
}
