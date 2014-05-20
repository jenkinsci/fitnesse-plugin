package hudson.plugins.fitnesse;

import hudson.Launcher.ProcStarter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Class needed to confirm that 
 * @author tim
 *
 */
final class StdConsole {
	private final ByteArrayOutputStream stdout;
	private final ByteArrayOutputStream stderr;

	public StdConsole() {
		this(new ByteArrayOutputStream(), new ByteArrayOutputStream());
	}

	public void provideStdOutAndStdErrFor(ProcStarter procStarter) {
		procStarter.stdout(stdout).stderr(stderr);
	}

	public StdConsole(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
		this.stdout = stdout;
		this.stderr = stderr;
	}

  public void log(PrintStream logger)
  {
    logger.println(stdout.toByteArray());
    stdout.reset();
    logger.println(stderr.toByteArray());
    stderr.reset();
	}

}