package hudson.plugins.fitnesse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Test;

public class StdConsoleTest {
	
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Test
	public void stdConsoleShouldLogIncrementalOutputToPrintStream() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		StdConsole console = new StdConsole(stdout, stderr);
		simulateWriteToStdOut(stdout, "hello std out");
		simulateWriteToStdErr(stderr, "hello std err");
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		PrintStream logger = new PrintStream(log);
		console.logOutput(logger);
		Assert.assertEquals(joinWithLineSeparators("hello std out", "hello std err"),
				new String(log.toByteArray()));

		log.reset();
		simulateWriteToStdOut(stdout, "yo std out"); 
		simulateWriteToStdErr(stderr, "yo std err");
		console.logOutput(logger);
		Assert.assertEquals(joinWithLineSeparators("yo std out","yo std err"), 
				new String(log.toByteArray()));
	}

	private String joinWithLineSeparators(String first, String second) {
		return first + LINE_SEPARATOR + second + LINE_SEPARATOR;
	}

	private void simulateWriteToStdOut(OutputStream stdout, String msgToStdOut) throws IOException {
		stdout.write(msgToStdOut.getBytes());
	}

	private void simulateWriteToStdErr(OutputStream stderr, String msgToStdErr) throws IOException {
		stderr.write(msgToStdErr.getBytes());
	}
	
	@Test
	public void stdConsoleShouldKnowIfThereHasBeenOutputOnStdOut() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertTrue(console.noOutputOnStdOut());
		simulateWriteToStdErr(stderr, "ping");
		Assert.assertTrue(console.noOutputOnStdOut());
		simulateWriteToStdOut(stdout, "ping");
		Assert.assertFalse(console.noOutputOnStdOut());
	}
	
	@Test
	public void stdConsoleShouldKnowIfThereHasBeenOutputOnStdErr() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertFalse(console.outputOnStdErr());
		simulateWriteToStdOut(stdout, "ping");
		Assert.assertFalse(console.outputOnStdErr());
		simulateWriteToStdErr(stderr, "ping");
		Assert.assertTrue(console.outputOnStdErr());
	}
}
