package hudson.plugins.fitnesse;

import junit.framework.Assert;
import hudson.model.FreeStyleBuild;
import hudson.plugins.fitnesse.NativePageCounts.Counts;

import org.jvnet.hudson.test.HudsonTestCase;

public class HudsonDependentTest extends HudsonTestCase {

	public void testOwnerOfParentResultsShouldBeOwnerOfChildResults() throws Exception {
		FitnesseResults parent = new FitnesseResults((Counts)null);
		FitnesseResults child = new FitnesseResults((Counts)null);
		parent.setOwner(new FreeStyleBuild(createFreeStyleProject()));
		parent.addDetail(child);
		Assert.assertSame(parent.getOwner(), child.getOwner());
	}
}
