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
	private int stdOutMark;
	private int stdErrMark;

	public StdConsole() {
		this(new ByteArrayOutputStream(), new ByteArrayOutputStream());
	}

	public void provideStdOutAndStdErrFor(ProcStarter procStarter) {
		procStarter.stdout(stdout).stderr(stderr);
	}

	public StdConsole(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
		this.stdout = stdout;
		this.stderr = stderr;
		this.stdOutMark = 0;
		this.stdErrMark = 0;
	}

	public void logIncrementalOutput(PrintStream logger) {
		if (incrementalOutputOnStdOut())
			stdOutMark = logAndMark(logger, stdout, stdOutMark);
		if (incrementalOutputOnStdErr())
			stdErrMark = logAndMark(logger, stderr, stdErrMark);
	}

	private int logAndMark(PrintStream logger, ByteArrayOutputStream bytes, int mark) {
    	int size = bytes.size(); 
    	logger.println(new String(bytes.toByteArray(), mark, size - mark));
    	return size;
	}

	public boolean noOutputOnStdOut() {
		return stdout.size()==0 ;
	}

	public boolean outputOnStdErr() {
		return stderr.size() > 0;
	}

	public boolean noIncrementalOutput() {
		return !incrementalOutputOnStdOut() && !incrementalOutputOnStdErr();
	}

	public boolean incrementalOutputOnStdErr() {
		return stderr.size() != stdErrMark;
	}

	public boolean incrementalOutputOnStdOut() {
		return stdout.size() != stdOutMark;
	}
}