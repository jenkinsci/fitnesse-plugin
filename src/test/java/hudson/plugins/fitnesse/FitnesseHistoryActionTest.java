package hudson.plugins.fitnesse;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static hudson.plugins.fitnesse.NativePageCounts.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class FitnesseHistoryActionTest {

    @Test
    public void pagesShouldBeOrderedByErraticness() {

		List<FitnesseResults> builds = builds();

		addFailingTest("FailAllTheTime", builds);
		addPassingTest("PassAllTheTime", builds);
		addErraticTest("Erratic", builds);

        FitnesseHistoryAction fitnesseHistoryAction = new FitnesseHistoryAction(null);
		List<String> pages = fitnesseHistoryAction.getPages(builds);

		assertThat(pages, contains("Erratic", "FailAllTheTime", "PassAllTheTime"));
    }

	@Test
	public void suitePageNoFailNoPass() {
		List<FitnesseResults> builds = builds();

		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts("Suite", "", 0, 0, 0, 0, "SomeContent")));
			build.addChild(new FitnesseResults(new Counts("OtherSuite", "", 0, 0, 0, 0, "OtherContent")));
		}

		FitnesseHistoryAction fitnesseHistoryAction = new FitnesseHistoryAction(null);
		List<String> pages = fitnesseHistoryAction.getPages(builds);
		assertThat(pages, containsInAnyOrder("Suite", "OtherSuite"));
	}

	private ArrayList<FitnesseResults> builds() {
		return Lists.newArrayList(
				new FitnesseResults((Counts) null),
				new FitnesseResults((Counts) null),
				new FitnesseResults((Counts) null),
				new FitnesseResults((Counts) null),
				new FitnesseResults((Counts) null));
	}

	private void addErraticTest(String page, List<FitnesseResults> builds) {
		int wrong = 0;
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, wrong, 0, 0, page)));
			if (wrong == 0) {
				wrong = 3;
			} else {
				wrong = 0;
			}
		}
	}

	private void addFailingTest(String page, List<FitnesseResults> builds) {
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, 5, 0, 0, page)));
		}
	}

	private void addPassingTest(String page, List<FitnesseResults> builds) {
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, 0, 0, 0, page)));
		}
	}
}
