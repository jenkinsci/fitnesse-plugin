package hudson.plugins.fitnesse;

public class RunnerWithTimeOut {
	static final int POLL_EVERY_MILLIS = 500;
	private final int timeOutMillis;
	
	private long waitedAlready;

	public RunnerWithTimeOut(int timeoutMillis) {
		this.timeOutMillis = timeoutMillis;
	}

	public void run(Runnable runnable) throws InterruptedException {
		int sleepMillis = timeOutMillis < POLL_EVERY_MILLIS ? timeOutMillis : POLL_EVERY_MILLIS;

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
}
