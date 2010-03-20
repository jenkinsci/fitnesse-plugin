package hudson.plugins.fitnesse;

public class RunnerWithTimeOut implements Resettable {
	static final int POLL_EVERY_MILLIS = 500;
	private final int explodeInMillis;
	private long waitedAlready;

	public RunnerWithTimeOut(int explodeInMillis) {
		this.explodeInMillis = explodeInMillis;
	}

	public void run(Runnable runnable) throws InterruptedException {
		Thread thread = new Thread(runnable);
		thread.start();
		reset();
		int sleepMillis = explodeInMillis < POLL_EVERY_MILLIS ? explodeInMillis : POLL_EVERY_MILLIS;
		do {
			Thread.sleep(sleepMillis);
			waitedAlready += sleepMillis;
		} while (thread.isAlive() && waitedAlready < explodeInMillis);
		
		if (thread.isAlive()) {
			thread.interrupt();
			throw new InterruptedException("Waited " + waitedAlready + "ms");
		}
	}

	public void reset() {
		waitedAlready = 0;
	}
}

interface Resettable {
	void reset();
}
