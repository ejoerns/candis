package candis.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage random id based on SecureRandom.
 *
 * @author Enrico Joerns
 */
public class RandomID implements Serializable {

	/// Length of ID in bits
	public static final int ID_LENGTH = 4096;
	private byte bytes[] = new byte[ID_LENGTH / 8];

	public RandomID() {
	}

	/**
	 * Copy constructor
	 *
	 * @param id
	 */
	public RandomID(RandomID id) {
		setBytes(id.getBytes());
	}

	/**
	 * Writes random ID to file.
	 *
	 * @param file Name of file to store data to
	 * @param id
	 */
	public static void writeToFile(final File file, final RandomID id) throws FileNotFoundException {
		OutputStream out = new FileOutputStream(file);
		try {
			out.write(id.bytes);
		} catch (IOException ex) {
			Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
				Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void writeToFile(final String file, final RandomID id) throws FileNotFoundException {
		writeToFile(new File(file), id);
	}

	/**
	 * Reads random ID from file.
	 *
	 * @param file Name of file to read data from
	 * @return
	 */
	public static RandomID readFromFile(final File file) throws FileNotFoundException {
		InputStream in = new FileInputStream(file);
		RandomID randId = new RandomID();
		try {
			in.read(randId.bytes);
		} catch (IOException ex) {
			Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
				Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return randId;
	}

	public static RandomID readFromFile(final String file) throws FileNotFoundException {
		return readFromFile(new File(file));
	}

	/**
	 * Creates new id file with 4096 bit random sequence.
	 *
	 * @param file
	 * @return RandomID that was written to file
	 */
	public static RandomID init(final File file) {
		SecureRandom random = new SecureRandom();
		RandomID id = new RandomID();
		random.nextBytes(id.bytes);

		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(id.bytes);
		} catch (IOException ex) {
			Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
				Logger.getLogger(RandomID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return id;
	}

	public static RandomID init(final String file) {
		return init(new File(file));
	}

	/**
	 *
	 * @param b
	 */
	public byte[] getBytes() {
		return bytes;
	}

	public final void setBytes(byte[] b) {
		bytes = b;
	}
}
