package hudson.plugins.fitnesse;

import java.util.Collection;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseResultsTest {
	private static final Counts FIRST = new Counts("", "20100313174438", 1, 2, 3, 4);
	private static final Counts SECOND = new Counts("", "20100313174439", 1, 2, 3, 4);
	
	private static final FitnesseResults RIGHT = new FitnesseResults(new Counts("", "20100320184439", 1, 0, 0, 0));
	private static final FitnesseResults WRONG = new FitnesseResults(new Counts("", "20100320184439", 0, 1, 0, 0));
	private static final FitnesseResults ALSO_WRONG = new FitnesseResults(new Counts("", "20100320184439", 1, 1, 0, 0));
	private static final FitnesseResults IGNORED = new FitnesseResults(new Counts("", "20100320184439", 0, 0, 1, 0));
	private static final FitnesseResults ALSO_IGNORED = new FitnesseResults(new Counts("", "20100320184439", 1, 0, 1, 0));
	private static final FitnesseResults EXCEPTION = new FitnesseResults(new Counts("", "20100320184439", 0, 0, 0, 1));
	private static final FitnesseResults ALSO_EXCEPTION = new FitnesseResults(new Counts("", "20100320184439", 1, 0, 0, 1));
	private static final FitnesseResults NO_OP = new FitnesseResults(new Counts("", "20100320184439", 0, 0, 0, 0));

	@Test
	public void isEarlierThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(FIRST);
		FitnesseResults second = new FitnesseResults(SECOND);
		Assert.assertTrue(first.isEarlierThan(second));
		Assert.assertFalse(second.isEarlierThan(first));
		Assert.assertFalse(second.isEarlierThan(second));
	}
	
	@Test
	public void isLaterThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(FIRST);
		FitnesseResults second = new FitnesseResults(SECOND);
		Assert.assertTrue(second.isLaterThan(first));
		Assert.assertFalse(first.isLaterThan(second));
		Assert.assertFalse(first.isLaterThan(first));
	}

	@Test
	public void secondsAfterThanShouldDependOnCounts() {
		FitnesseResults first = new FitnesseResults(FIRST);
		FitnesseResults second = new FitnesseResults(SECOND);
		Assert.assertEquals(1000, second.millisAfter(first));
		Assert.assertEquals(-1000, first.millisAfter(second));
		Assert.assertEquals(0, second.millisAfter(second));
	}
	
	@Test
	public void durationShouldBeDifferenceBetweenEarliestAndLatestResults() {
		FitnesseResults first = new FitnesseResults(FIRST);
		FitnesseResults second = new FitnesseResults(SECOND);
		FitnesseResults summary = new FitnesseResults((Counts)null);
		summary.addDetail(first);
		summary.addDetail(second);
		Assert.assertEquals(1.0f, summary.getDuration());
	}
	
	@Test
	public void failedTestsShouldIncludeResultsWithFailures() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> failedTests = summary.getFailedTests();
		Assert.assertEquals(2, failedTests.size());
		Assert.assertTrue(failedTests.contains(WRONG));
		Assert.assertTrue(failedTests.contains(ALSO_WRONG));
	}

	@Test
	public void skippedTestsShouldIncludeResultsIgnoredWithExceptionsOrWithNoTotal() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> skippedTests = summary.getSkippedTests();
		Assert.assertEquals(5, skippedTests.size());
		Assert.assertTrue(skippedTests.contains(IGNORED));
		Assert.assertTrue(skippedTests.contains(ALSO_IGNORED));
		Assert.assertTrue(skippedTests.contains(EXCEPTION));
		Assert.assertTrue(skippedTests.contains(ALSO_EXCEPTION));
		Assert.assertTrue(skippedTests.contains(NO_OP));
	}

	@Test
	public void passedTestsShouldIncludeResultsWithAllRight() {
		FitnesseResults summary = setUpSummaryResults();
		Collection<FitnesseResults> passedTests = summary.getPassedTests();
		Assert.assertEquals(1, passedTests.size());
		Assert.assertTrue(passedTests.contains(RIGHT));
	}

	private FitnesseResults setUpSummaryResults() {
		FitnesseResults summary = new FitnesseResults((Counts)null);
		summary.addDetail(RIGHT);
		summary.addDetail(WRONG);
		summary.addDetail(ALSO_WRONG);
		summary.addDetail(IGNORED);
		summary.addDetail(ALSO_IGNORED);
		summary.addDetail(EXCEPTION);
		summary.addDetail(ALSO_EXCEPTION);
		summary.addDetail(NO_OP);
		return summary;
	}
}
