package hudson.plugins.fitnesse;

import org.junit.Assert;
import org.junit.Test;

public class RunnerWithTimeOutTest {
	private RunnerWithTimeOut runner;

	private boolean wasRun;
	@Test
	public void startedRunnerShouldRunRunnable() throws Exception {
		wasRun = false;
		Runnable runnable = new Runnable() {
			public void run() {
				wasRun = true;
			}
		};
		runner = new RunnerWithTimeOut(1000);
		runner.run(runnable);
		Assert.assertTrue(wasRun);
	}

	@Test(expected=InterruptedException.class)
	public void startedRunnerShouldThrowExceptionAfterTimeout() throws Exception {
		runner = new RunnerWithTimeOut(100);
		runner.run(new Runnable() {
			public void run() {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					// swallow
				}
			}
		});
	}

	@Test
	public void resettingRunnerShouldRestartTimeOutCountdown() throws Exception {
		runner = new RunnerWithTimeOut(600);
		final Resettable resettable = runner;
		runner.run(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					resettable.reset();
					Thread.sleep(300);
					resettable.reset();
					Thread.sleep(400);
				} catch (InterruptedException e) {
					// swallow
				}
			}
		});
	}
	
	private boolean eventWasFired;
	@Test
	public void resettingRunnerWithResetEventShouldFireResetEvent() throws Exception {
		eventWasFired = false;
		runner = new RunnerWithTimeOut(100);
		final Resettable resettable = runner;
		runner.run(new Runnable() {
			public void run() {
				resettable.reset();
			}
		}, new ResetEvent() {
			public void onReset() {
				eventWasFired = true;
			}
		});
		Assert.assertTrue(eventWasFired);
	}
}
