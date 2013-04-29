package candis.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage random id based on SecureRandom.
 *
 * @author Enrico Joerns
 */
public class DroidID {

  /// Length of ID in bits
  public static final int ID_LENGTH = 4096;
  private final byte[] mBytes;

  private DroidID(byte[] bytes) {
    mBytes = bytes;
  }

  /**
   * Reads random ID from file.
   *
   * @param file Name of file to read data from
   * @return
   */
  public static DroidID readFromFile(final File file) throws FileNotFoundException {
    byte[] bytes = new byte[ID_LENGTH / 8];
    InputStream in = new FileInputStream(file);
    try {
      in.read(bytes);
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
    return new DroidID(bytes);
  }

  public static DroidID readFromFile(final String file) throws FileNotFoundException {
    return readFromFile(new File(file));
  }

  /**
   * Generates and saves new id with ID_LENGTH bit random sequence.
   *
   * @param file File name for new generated id file
   * @return RandomID that was written to file
   */
  public static DroidID generate(final File file) throws FileNotFoundException {
    byte[] bytes = new byte[ID_LENGTH / 8];
    SecureRandom random = new SecureRandom();
    random.nextBytes(bytes);

    DroidID id = new DroidID(bytes);

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
    return id;
  }

  public static DroidID generate(final String file) throws FileNotFoundException {
    return generate(new File(file));
  }

  /**
   * Returns the byte array representing the droid ID
   *
   * @return ID array of length ID_LENGTH
   */
  public byte[] getBytes() {
    return mBytes;
  }

  /**
   * Returns SHA-1 hash of the ID.
   *
   * For non-critical identification the full id normally is to long.
   * For this the SHA-1 of the id is intended.
   *
   * @return SHA-1 hash of id bytes
   */
  public String toSHA1() {
    return Utilities.toSHA1String(mBytes);
  }

  /**
   * Returns string representation of ID.
   *
   * This is identical to toSHA1().
   *
   * @return String representaiton of ID
   */
  @Override
  public String toString() {
    return toSHA1();
  }
}
