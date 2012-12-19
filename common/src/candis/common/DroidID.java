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
public class DroidID implements Serializable {

	/// Length of ID in bits
	public static final int ID_LENGTH = 4096;
	private final byte mBytes[];

	public DroidID() {
		mBytes = new byte[ID_LENGTH / 8];
	}

	/**
	 * Copy constructor
	 *
	 * @param id
	 */
	public DroidID(final DroidID id) {
		mBytes = id.getBytes();
	}

	/**
	 * Writes random ID to file.
	 *
	 * @param file Name of file to store data to
	 * @param id
	 */
	public static void writeToFile(final File file, final DroidID id) throws FileNotFoundException {
		OutputStream out = new FileOutputStream(file);
		try {
			out.write(id.mBytes);
		}
		catch (IOException ex) {
			Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException ex) {
				Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void writeToFile(final String file, final DroidID id) throws FileNotFoundException {
		writeToFile(new File(file), id);
	}

	/**
	 * Reads random ID from file.
	 *
	 * @param file Name of file to read data from
	 * @return
	 */
	public static DroidID readFromFile(final File file) throws FileNotFoundException {
		InputStream in = new FileInputStream(file);
		DroidID randId = new DroidID();
		try {
			in.read(randId.mBytes);
		}
		catch (IOException ex) {
			Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
				Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return randId;
	}

	public static DroidID readFromFile(final String file) throws FileNotFoundException {
		return readFromFile(new File(file));
	}

	/**
	 * Creates new id file with 4096 bit random sequence.
	 *
	 * @param file
	 * @return RandomID that was written to file
	 */
	public static DroidID init(final File file) {
		SecureRandom random = new SecureRandom();
		DroidID id = new DroidID();
		random.nextBytes(id.mBytes);

		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(id.mBytes);
		}
		catch (IOException ex) {
			Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException ex) {
				Logger.getLogger(DroidID.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return id;
	}

	public static DroidID init(final String file) {
		return init(new File(file));
	}

	/**
	 *
	 * @param b
	 */
	public byte[] getBytes() {
		return mBytes;
	}

	public String toSHA1() {
		return Utilities.toSHA1String(mBytes);
	}

	@Override
	public String toString() {
		return toSHA1();
	}
}
