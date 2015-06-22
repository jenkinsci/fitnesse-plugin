package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class FitnesseHistoryActionTest {

	@Test
	public void pagesShouldBeOrderedByErraticness() {
		List<FitnesseResults> builds = builds();

		addFailingTest("FailAllTheTime", builds);
		addPassingTest("PassAllTheTime", builds);
		addErraticTest("Erratic", builds);

		Map<String, List<String>> pages = FitnesseHistoryAction.extractPages(builds);

		assertThat(pages.get(NULL_FITNESSE_RESULT_NAME),
				contains("FailAllTheTime", "Erratic", "PassAllTheTime"));
	}

	@Test
	public void suitePageNoFailNoPass() {
		List<FitnesseResults> builds = builds();

		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts("Suite", "", 0, 0, 0, 0, 0, "SomeContent")));
			build.addChild(new FitnesseResults(new Counts("OtherSuite", "", 0, 0, 0, 0, 0, "OtherContent")));
		}

		Map<String, List<String>> pages = FitnesseHistoryAction.extractPages(builds);
		
		assertThat(pages.get(NULL_FITNESSE_RESULT_NAME), containsInAnyOrder("Suite", "OtherSuite"));
	}

	/*
	 * PRIVATE
	 */

	private static final String NULL_FITNESSE_RESULT_NAME = "null";

	private static FitnesseResults getNewFitnesseResult() {
		return new FitnesseResults( //
				new Counts(NULL_FITNESSE_RESULT_NAME, "", 0, 0, 0, 0, 0, ""));
	}

	private static ArrayList<FitnesseResults> builds() {
		return Lists.newArrayList(getNewFitnesseResult(), getNewFitnesseResult(), getNewFitnesseResult(),
				getNewFitnesseResult(), getNewFitnesseResult());
	}

	private void addErraticTest(String page, List<FitnesseResults> builds) {
		int wrong = 0;
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, wrong, 0, 0, 0, page)));
			if (wrong == 0) {
				wrong = 3;
			} else {
				wrong = 0;
			}
		}
	}

	private static void addFailingTest(String page, List<FitnesseResults> builds) {
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, 5, 0, 0, 0, page)));
		}
	}

	private static void addPassingTest(String page, List<FitnesseResults> builds) {
		for (FitnesseResults build : builds) {
			build.addChild(new FitnesseResults(new Counts(page, "", 3, 0, 0, 0, 0, page)));
		}
	}
}
