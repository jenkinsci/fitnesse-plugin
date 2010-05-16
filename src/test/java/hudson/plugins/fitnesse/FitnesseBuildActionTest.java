package hudson.plugins.fitnesse;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseBuildActionTest {
	@Test
	public void getLinkForNullActionShouldBePageName() {
		Assert.assertEquals("pagename", 
				FitnesseBuildAction.NULL_ACTION.getLinkFor("pagename"));
		Assert.assertEquals("pagename", 
				FitnesseBuildAction.NULL_ACTION.getLinkFor("pagename", "http://host:8080"));
	}

	@Test
	public void getLinkForPageWhenBuildStartedFitnesseShouldBePageName() {
		FitnesseBuildAction action = new FitnesseBuildAction(true, "host", 8900);
		Assert.assertEquals("pagename", 
				action.getLinkFor("pagename"));
		Assert.assertEquals("pagename", 
				action.getLinkFor("pagename", "http://host:8080"));
	}

	@Test
	public void getLinkForPageWhenBuildNotStartedFitnesseShouldBeHtmlALink() {
		FitnesseBuildAction action = new FitnesseBuildAction(false, "host", 8900);
		Assert.assertEquals("<a href=\"http://host:8900/pagename\">pagename</a>", 
				action.getLinkFor("pagename"));
	}
	
	@Test
	public void getLinkForPageShouldSubsitituteHostNameForLocalHost() {
		FitnesseBuildAction action = new FitnesseBuildAction(false, "localhost", 8900);
		Assert.assertEquals("<a href=\"http://host:8900/pagename\">pagename</a>", 
				action.getLinkFor("pagename", "http://host:8080"));
	}
	
	@Test
	public void getLinkForPageShouldNotSubsitituteHostNameForNonLocalHost() {
			FitnesseBuildAction action = new FitnesseBuildAction(false, "host1", 8900);
			Assert.assertEquals("<a href=\"http://host1:8900/pagename\">pagename</a>", 
					action.getLinkFor("pagename", "http://host2:8080"));
	}
	
	@Test
	public void getLinkForPageShouldNotSubsitituteNullHostNameForLocalHost() {
		FitnesseBuildAction action = new FitnesseBuildAction(false, "localhost", 8900);
		Assert.assertEquals("<a href=\"http://localhost:8900/pagename\">pagename</a>", 
				action.getLinkFor("pagename", null));
	}

}
