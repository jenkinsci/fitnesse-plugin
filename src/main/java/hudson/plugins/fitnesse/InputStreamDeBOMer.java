package hudson.plugins.fitnesse;

import hudson.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * The "need" for a DeBOMer is explained at 
 * {@linkplain http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058}
 */
public class InputStreamDeBOMer {
	/**
	 * BOM definitions at {@linkplain http://www.unicode.org/faq/utf_bom.html#BOM}
	 */
	public static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	public static final byte[] UTF32LE_BOM = new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00};
	public static final byte[] UTF16LE_BOM = new byte[] {(byte) 0xFF, (byte) 0xFE};
	public static final byte[] UTF16BE_BOM = new byte[] {(byte) 0xFE, (byte) 0xFF};
	public static final byte[] UTF32BE_BOM = new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF};

	public static InputStream deBOM(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(in, baos);
		byte[] bytes = baos.toByteArray();
		int skip = skip_bom(UTF8_BOM, bytes);
		if (skip == 0) skip = skip_bom(UTF32LE_BOM, bytes);
		if (skip == 0) skip = skip_bom(UTF16LE_BOM, bytes);
		if (skip == 0) skip = skip_bom(UTF16BE_BOM, bytes);
		if (skip == 0) skip = skip_bom(UTF32BE_BOM, bytes);
		return new ByteArrayInputStream(bytes, skip, bytes.length - skip);
	}

	private static int skip_bom(byte[] bom, byte[] bytes) {
		if (bytes.length < bom.length) return 0;
		
		for (int i=0; i < bom.length; ++i) {
			if (bytes[i] != bom[i]) return 0;
		}
		
		return bom.length;
	}
}