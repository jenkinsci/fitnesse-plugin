package hudson.plugins.fitnesse;

public class RunnerWithTimeOut implements Resettable {
	static final int POLL_EVERY_MILLIS = 500;
	private final int timeOutMillis;
	
	private ResetEvent resetEvent;
	private long waitedAlready;

	public RunnerWithTimeOut(int timeoutMillis) {
		this.timeOutMillis = timeoutMillis;
	}

	public void run(Runnable runnable) throws InterruptedException {
		run(runnable, null);
	}
	
	public void run(Runnable runnable, ResetEvent resetEvent) throws InterruptedException {
		int sleepMillis = timeOutMillis < POLL_EVERY_MILLIS ? timeOutMillis : POLL_EVERY_MILLIS;
		this.resetEvent = null;
		reset();
		this.resetEvent = resetEvent;

		Thread thread = new Thread(runnable);
		thread.start();
		
		do {
			waitedAlready += sleepMillis;
			Thread.sleep(sleepMillis);
		} while (thread.isAlive() && waitedAlready < timeOutMillis);
		
		if (thread.isAlive()) {
			thread.interrupt();
			throw new InterruptedException("Waited " + waitedAlready + "ms");
		}
	}

	public void reset() {
		waitedAlready = 0;
		if (resetEvent != null) {
			resetEvent.onReset();
		}
	}
}

interface Resettable {
	void reset();
}

interface ResetEvent {
	void onReset();
}
