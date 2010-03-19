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

	public void logOutput(PrintStream logger) {
		stdOutMark = logAndMark(logger, stdout, stdOutMark);
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

	public boolean stdErrStartsWith(String prefix) {
		byte[] pBytes = prefix.getBytes();
		byte[] eBytes = stderr.toByteArray();
		if (pBytes.length > eBytes.length) return false;
		for (int i=0; i < pBytes.length; ++i) {
			if (pBytes[i] != eBytes[i]) return false;	
		}
		return true;
	}
}