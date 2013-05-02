package candis.common;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

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
  
  	/**
	 *
	 * @param in Inputstream to read from
	 * @param out Outputstream to copy to
	 * @throws IOException
	 */
	public static void copyInputStream(InputStream in, OutputStream out)
					throws IOException {
		final byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}

	/**
	 * Removes the filename extension from filename.
	 *
	 * @param fname filename to process
	 * @return filename without extension
	 */
	public static String removeFileExtension(String fname) {

		String filename;

		// Remove the path upto the filename.
		final int lastSepIdx = fname.lastIndexOf(System.getProperty("file.separator"));
		if (lastSepIdx == -1) {
			filename = fname;
		}
		else {
			filename = fname.substring(lastSepIdx + 1);
		}

		// Remove the extension.
		final int extensionIndex = filename.lastIndexOf('.');
		if (extensionIndex == -1) {
			return filename;
		}

		return filename.substring(0, extensionIndex);
	}  
  
	/**
	 * Finds all class names in a jar file.
	 *
	 * @param jarName Jar file to search in
	 * @return List of all full class names
	 */
	public static List<String> getClassNamesInJar(final File jarFile) {
		final List<String> classes = new ArrayList<String>();
		JarInputStream jarInputStream = null;
		try {
			jarInputStream = new JarInputStream(new FileInputStream(jarFile));
			JarEntry jarEntry;

			while (true) {
				jarEntry = jarInputStream.getNextJarEntry();
				if (jarEntry == null) {
					break;
				}
				if (jarEntry.getName().endsWith(".class")) {
//					LOGGER.log(Level.FINE, "Found class: " + jarEntry.getName().replaceAll("/", "\\."));
					classes.add(removeFileExtension(jarEntry.getName().replaceAll("/", "\\.")));
				}
			}
		}
		catch (Exception e) {

//			LOGGER.log(Level.WARNING, "hier ist was doof", e);
		}
		finally {
			try {
				if (jarInputStream != null) {
					jarInputStream.close();
				}
			}
			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
		return classes;
	}
}
