package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import org.junit.Assert;
import org.junit.Test;

public class FitnesseResultsTest {
	private static final Counts FIRST = new Counts("", "20100313174438", 0, 0, 0, 0);
	private static final Counts SECOND = new Counts("", "20100313174439", 0, 0, 0, 0);

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
}
