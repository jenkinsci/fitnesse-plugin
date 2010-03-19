package hudson.plugins.fitnesse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class InputStreamDeBOMerTest {
	final byte[] bytes = new byte[] {(byte)2, (byte)3, (byte)5, (byte)8, (byte)13}; 
	ByteArrayInputStream in; 

	@Test
	public void deBOMShouldIgnoreBOMlessInput() throws Exception {
		in = new ByteArrayInputStream(bytes);
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}
	
	@Test
	public void deBOMShouldRemoveUTF8BOM() throws Exception {
		in = new ByteArrayInputStream(addBOM(InputStreamDeBOMer.UTF8_BOM, bytes));
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}

	@Test
	public void deBOMShouldRemoveUTF16LEBOM() throws Exception {
		in = new ByteArrayInputStream(addBOM(InputStreamDeBOMer.UTF16LE_BOM, bytes));
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}

	@Test
	public void deBOMShouldRemoveUTF16BEBOM() throws Exception {
		in = new ByteArrayInputStream(addBOM(InputStreamDeBOMer.UTF16BE_BOM, bytes));
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}
	
	@Test
	public void deBOMShouldRemoveUTF32LEBOM() throws Exception {
		in = new ByteArrayInputStream(addBOM(InputStreamDeBOMer.UTF32LE_BOM, bytes));
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}
	
	@Test
	public void deBOMShouldRemoveUTF32BEBOM() throws Exception {
		in = new ByteArrayInputStream(addBOM(InputStreamDeBOMer.UTF32BE_BOM, bytes));
		InputStream stream = InputStreamDeBOMer.deBOM(in);
		assertBomSkipped(stream);
	}

	private byte[] addBOM(byte[] bom, byte[] tobytes) {
		byte[] out = new byte[bom.length + tobytes.length];
		for (int i=0; i < bom.length; ++i) {
			out[i] = bom[i];
		}
		for (int i=0; i < tobytes.length; ++i) {
			out[bom.length + i] = tobytes[i];
		}
		return out;
	}

	private void assertBomSkipped(InputStream stream) throws IOException {
		for (int i=0; i < bytes.length; ++i) {
			Assert.assertEquals(bytes[i], stream.read());
		}
	}
}
