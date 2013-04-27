package candis.client;

import candis.distributed.droid.DeviceProfile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.logging.Logger;

/**
 * Provides functionality to get static profile information from the device.
 *
 * @author Enrico Joerns
 */
public abstract class DeviceProfiler {

  private static final String TAG = DeviceProfiler.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  /**
   * Runs profiling tests and returns a StaticProfile object including all
   * collected informations.
   *
   * @return
   */
  public DeviceProfile profile() {
    return new DeviceProfile(
            getDeviecID(),
            getModel(),
            getMemorySize(),
            getNumCores(),
            0);// TODO: remove field
  }

  /**
   * Reads the entire memory size of the device
   *
   * @return Size in [bytes]
   */
  public abstract long getMemorySize();

  /**
   * Returns the number of cores available in this device, across all
   * processors. Requires: Ability to peruse the filesystem at
   * "/sys/devices/system/cpu"
   *
   * @return The number of cores, or 1 if failed to get result
   */
  public abstract int getNumCores();

  /**
   * Returns the device ID.
   *
   * This might be either the IMEI oder the MAC address
   *
   * @return
   */
  public abstract String getDeviecID();

  /**
   * Returns the device model.
   *
   * Might be used to lookup performance data from a table
   *
   * @return Model of device
   */
  public abstract String getModel();

  /**
   * Reads profile data from given file.
   *
   * @param f File to read profile from
   * @return StaticProfile instance
   */
  public static DeviceProfile readProfile(File f) {
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new FileInputStream(f));

      Object obj = ois.readObject();

      if (obj instanceof DeviceProfile) {
        return (DeviceProfile) obj;
      }
      else {
        LOGGER.severe("invalid profile file");
        return null;
      }
    }
    catch (StreamCorruptedException ex) {
      LOGGER.severe(ex.toString());
    }
    catch (FileNotFoundException ex) {
      LOGGER.severe("Profile file " + f + " not found!");
    }
    catch (IOException ex) {
      LOGGER.severe(ex.toString());
    }
    catch (ClassNotFoundException ex) {
      LOGGER.severe(ex.toString());
    }
    finally {
      if (ois != null) {
        try {
          ois.close();
        }
        catch (IOException ex) {
          LOGGER.severe(ex.toString());
        }
      }
    }
    return null;
  }

  /**
   * Writes StaticProfile to file
   *
   * @param f Store destination
   * @param p Profile to store
   */
  public static void writeProfile(File f, DeviceProfile p) {
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(f));
      oos.writeObject(p);
    }
    catch (FileNotFoundException ex) {
      LOGGER.severe(ex.toString());
    }
    catch (IOException ex) {
      LOGGER.severe(ex.toString());
    }
    finally {
      if (oos != null) {
        try {
          oos.close();
        }
        catch (IOException ex) {
          LOGGER.severe(ex.toString());
        }
      }
    }
  }
}
