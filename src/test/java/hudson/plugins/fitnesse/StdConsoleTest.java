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
		simulateWriteTo(stdout, "hello std out");
		simulateWriteTo(stderr, "hello std err");
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		PrintStream logger = new PrintStream(log);
		console.logIncrementalOutput(logger);
		Assert.assertEquals(joinWithLineSeparators("hello std out", "hello std err"),
				new String(log.toByteArray()));

		log.reset();
		simulateWriteTo(stdout, "yo std out"); 
		simulateWriteTo(stderr, "yo std err");
		console.logIncrementalOutput(logger);
		Assert.assertEquals(joinWithLineSeparators("yo std out","yo std err"), 
				new String(log.toByteArray()));
	}

	private String joinWithLineSeparators(String first, String second) {
		return first + LINE_SEPARATOR + second + LINE_SEPARATOR;
	}

	private void simulateWriteTo(OutputStream stream, String msg) throws IOException {
		stream.write(msg.getBytes());
	}

	@Test
	public void stdConsoleShouldKnowIfThereHasBeenOutputOnStdOut() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertTrue(console.noOutputOnStdOut());
		simulateWriteTo(stderr, "ping");
		Assert.assertTrue(console.noOutputOnStdOut());
		simulateWriteTo(stdout, "ping");
		Assert.assertFalse(console.noOutputOnStdOut());
	}
	
	@Test
	public void stdConsoleShouldKnowIfThereHasBeenOutputOnStdErr() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertFalse(console.outputOnStdErr());
		simulateWriteTo(stdout, "ping");
		Assert.assertFalse(console.outputOnStdErr());
		simulateWriteTo(stderr, "ping");
		Assert.assertTrue(console.outputOnStdErr());
	}
	
	@Test
	public void stdConsoleShouldRecogniseStdOutBytesAsIncrementalOutput() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertFalse(console.incrementalOutputOnStdOut());
		Assert.assertFalse(console.incrementalOutputOnStdErr());
		Assert.assertTrue(console.noIncrementalOutput());
		simulateWriteTo(stdout, "cheese");
		Assert.assertTrue(console.incrementalOutputOnStdOut());
		Assert.assertFalse(console.noIncrementalOutput());
		Assert.assertFalse(console.incrementalOutputOnStdErr());
	}

	@Test
	public void stdConsoleShouldRecogniseStdErrBytesAsIncrementalOutput() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		
		StdConsole console = new StdConsole(stdout, stderr);
		Assert.assertFalse(console.incrementalOutputOnStdOut());
		Assert.assertFalse(console.incrementalOutputOnStdErr());
		Assert.assertTrue(console.noIncrementalOutput());
		simulateWriteTo(stderr, "wensleydale");
		Assert.assertTrue(console.incrementalOutputOnStdErr());
		Assert.assertFalse(console.noIncrementalOutput());
		Assert.assertFalse(console.incrementalOutputOnStdOut());
	}
}
