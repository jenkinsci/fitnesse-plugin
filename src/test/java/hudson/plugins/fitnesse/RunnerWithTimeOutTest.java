package hudson.plugins.fitnesse;

import org.junit.Assert;
import org.junit.Test;

public class RunnerWithTimeOutTest {
	private boolean isRun;
	private RunnerWithTimeOut runner;

	@Test
	public void startedRunnerShouldRunRunnable() throws Exception {
		isRun = false;
		Runnable runnable = new Runnable() {
			public void run() {
				isRun = true;
			}
		};
		runner = new RunnerWithTimeOut(1000);
		runner.run(runnable);
		Assert.assertTrue(isRun);
	}

	@Test(expected=InterruptedException.class)
	public void startedRunnerShouldThrowExceptionAfterTimeout() throws Exception {
		runner = new RunnerWithTimeOut(100);
		runner.run(sleepFor(60000));
	}

	private Runnable sleepFor(final long sleepMillis) {
		return new Runnable() {
			public void run() {
				try {
					Thread.sleep(sleepMillis);
				} catch (InterruptedException e) {
					// swallow
				}
			}
		};
	}

	@Test
	public void resettingRunnerShouldRestartTimeOutCountdown() throws Exception {
		runner = new RunnerWithTimeOut(1000);
		final Resettable resettable = runner;
		runner.run(new Runnable() {
			public void run() {
				try {
					Thread.sleep(550);
					resettable.reset();
					Thread.sleep(550);
				} catch (InterruptedException e) {
					// swallow
				}
			}
		});
	}
}
