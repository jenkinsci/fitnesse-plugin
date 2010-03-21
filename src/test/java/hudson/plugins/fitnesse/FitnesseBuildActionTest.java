package hudson.plugins.fitnesse;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseBuildActionTest {
	@Test
	public void getLinkForNullActionShouldBePageName() {
		Assert.assertEquals("pagename", 
				FitnesseBuildAction.NULL_ACTION.getLinkFor("pagename"));
	}

	@Test
	public void getLinkForPageWhenBuildStartedFitnesseShouldBePageName() {
		FitnesseBuildAction action = new FitnesseBuildAction(true, "host", 8900);
		Assert.assertEquals("pagename", 
				action.getLinkFor("pagename"));
	}

	@Test
	public void getLinkForPageWhenBuildStartedFitnesseShouldBeHtmlALink() {
		FitnesseBuildAction action = new FitnesseBuildAction(false, "host", 8900);
		Assert.assertEquals("<a href=\"http://host:8900/pagename\">pagename</a>", 
				action.getLinkFor("pagename"));
	}

}
