package candis.common;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Enrico Joerns
 */
public class Utilities {

	private static final String TAG = "Utilities";

	/**
	 * Converts a byte to hex digit and writes to the supplied buffer.
	 *
	 * @param b
	 * @param buf
	 */
	public static void byte2hex(final byte b, final StringBuffer buf) {
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		int high = (b & 240) >> 4;
		int low = b & 15;
		buf.append(hexChars[high]);
		buf.append(hexChars[low]);
	}

	/**
	 *
	 * @param block
	 * @return
	 */
	public static String toHexString(final byte[] block) {
		StringBuffer buf = new StringBuffer();
		int len = block.length;
		for (int i = 0; i < len; i++) {
			byte2hex(block[i], buf);
			if (i < len - 1) {
				buf.append(":");
			}
		}
		return buf.toString();
	}

	public static String toSHA1String(final byte[] block) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return Utilities.toHexString(md.digest(block));
	}
}
