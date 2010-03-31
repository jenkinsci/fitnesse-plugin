package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@linkplain http://github.com/unclebob/fitnesse/blob/master/src/fit/Counts.java}
 * The "official" spec for tallying Counts (the results of assertion elements within a single page) 
 * into an overall Count (for the page as a whole) is hidden away in the fitnesse code base :-( 
 * Here is the pseduo code from tallyCounts() in the URL above.
 * - if any wrong assertions within page: page is wrong
 * - else if any exceptions thrown by assertions within page: page is exception
 * - else if any ignores within page and no right within page: page is ignore
 * - else: page is right
 */
public class FitnesseResultsTest {
	private static final FitnesseResults[] WRONG = new FitnesseResults[] { 
		resultsForCounts(0, 1, 0, 0),
		resultsForCounts(1, 1, 0, 0),
		resultsForCounts(0, 1, 1, 0),
		resultsForCounts(0, 1, 0, 1),
		resultsForCounts(1, 1, 1, 0),
		resultsForCounts(1, 1, 0, 1),
		resultsForCounts(0, 1, 1, 1),
		resultsForCounts(1, 1, 1, 1)
	};
	
	private static final FitnesseResults[] EXCEPTION = new FitnesseResults[] {
		resultsForCounts(0, 0, 0, 1),
		resultsForCounts(0, 0, 1, 1),
		resultsForCounts(1, 0, 0, 1),
		resultsForCounts(1, 0, 1, 1),
	};
	private static final FitnesseResults[] IGNORED = new FitnesseResults[] {
		resultsForCounts(0, 0, 1, 0),
		resultsForCounts(0, 0, 0, 0)
	};
	private static final FitnesseResults[] RIGHT = new FitnesseResults[] {
		resultsForCounts(1, 0, 0, 0),
		resultsForCounts(1, 0, 1, 0)
	};

	private static FitnesseResults resultsForCounts(int right, int wrong, int ignored, int exceptions) {
		return new FitnesseResults(new Counts("", "20100320184439", right, wrong, ignored, exceptions)); 
	}
	
	@Test
	public void wrongCountsShouldBeFailedOverall() {
		for (FitnesseResults results : WRONG) {
			Assert.assertTrue(results.getHeadlineText(), results.isFailedOverall());
			Assert.assertFalse(results.getHeadlineText(), results.isPassedOverall());
			Assert.assertFalse(results.getHeadlineText(), results.isSkippedOverall());
		}
	}

	@Test
	public void exceptionCountsShouldBeSkipped() {
		for (FitnesseResults results : EXCEPTION) {
			Assert.assertFalse(results.getHeadlineText(), results.isFailedOverall());
			Assert.assertTrue(results.getHeadlineText(), results.isSkippedOverall());
			Assert.assertFalse(results.getHeadlineText(), results.isPassedOverall());
		}
	}
	
	@Test
	public void ignoredCountsShouldBeSkipped() {
		for (FitnesseResults results : IGNORED) {
			Assert.assertFalse(results.getHeadlineText(), results.isFailedOverall());
			Assert.assertTrue(results.getHeadlineText(), results.isSkippedOverall());
			Assert.assertFalse(results.getHeadlineText(), results.isPassedOverall());
		}
	}
	
	@Test
	public void rightCountsShouldBePassed() {
		for (FitnesseResults results : RIGHT) {
			Assert.assertFalse(results.getHeadlineText(), results.isFailedOverall());
			Assert.assertFalse(results.getHeadlineText(), results.isSkippedOverall());
			Assert.assertTrue(results.getHeadlineText(), results.isPassedOverall());
		}
	}
	
	@Test
	public void failedTestsShouldIncludeCountsWrong() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> failedTests = summary.getFailedTests();
		Assert.assertEquals(WRONG.length, failedTests.size());
		for (FitnesseResults results : WRONG) {
			Assert.assertTrue(results.getHeadlineText(), failedTests.contains(results));
		}
	}

	@Test
	public void skippedTestsShouldIncludeCountsIgnoredOrExceptions() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> skippedTests = summary.getSkippedTests();
		Assert.assertEquals(EXCEPTION.length + IGNORED.length, skippedTests.size());
		for (FitnesseResults results : EXCEPTION) {
			Assert.assertTrue(results.getHeadlineText(), skippedTests.contains(results));
		}
		for (FitnesseResults results : IGNORED) {
			Assert.assertTrue(results.getHeadlineText(), skippedTests.contains(results));
		}
	}

	@Test
	public void passedTestsShouldIncludeCountsRight() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> passedTests = summary.getPassedTests();
		Assert.assertEquals(RIGHT.length, passedTests.size());
		for (FitnesseResults results : RIGHT) {
			Assert.assertTrue(results.getHeadlineText(), passedTests.contains(results));
		}
	}

	private FitnesseResults setUpSummaryResults() {
		FitnesseResults summary = new FitnesseResults((Counts)null);
		for (FitnesseResults results : RIGHT) {
			summary.addDetail(results);
		}
		for (FitnesseResults results : WRONG) {
			summary.addDetail(results);
		}
		for (FitnesseResults results : IGNORED) {
			summary.addDetail(results);
		}
		for (FitnesseResults results : EXCEPTION) {
			summary.addDetail(results);
		}
		return summary;
	}

	private static final Counts BEFORE = new Counts("", "20100313174438", 1, 2, 3, 4);
	private static final Counts AFTER = new Counts("", "20100313174439", 1, 2, 3, 4);
	
	@Test
	public void isEarlierThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(BEFORE);
		FitnesseResults second = new FitnesseResults(AFTER);
		Assert.assertTrue(first.isEarlierThan(second));
		Assert.assertFalse(second.isEarlierThan(first));
		Assert.assertFalse(second.isEarlierThan(second));
	}
	
	@Test
	public void isLaterThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(BEFORE);
		FitnesseResults second = new FitnesseResults(AFTER);
		Assert.assertTrue(second.isLaterThan(first));
		Assert.assertFalse(first.isLaterThan(second));
		Assert.assertFalse(first.isLaterThan(first));
	}

	@Test
	public void secondsAfterThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(BEFORE);
		FitnesseResults second = new FitnesseResults(AFTER);
		Assert.assertEquals(1000, second.millisAfter(first));
		Assert.assertEquals(-1000, first.millisAfter(second));
		Assert.assertEquals(0, second.millisAfter(second));
	}
	
	@Test
	public void durationShouldBeDifferenceBetweenEarliestAndLatestResults() {
		FitnesseResults first = new FitnesseResults(BEFORE);
		FitnesseResults second = new FitnesseResults(AFTER);
		FitnesseResults summary = new FitnesseResults((Counts)null);
		summary.addDetail(first);
		summary.addDetail(second);
		Assert.assertEquals(1.0f, summary.getDuration());
	}
}
