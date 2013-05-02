package candis.client;

import candis.distributed.droid.DeviceProfile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides functionality to get static getProfile information from the device.
 *
 * @author Enrico Joerns
 */
public abstract class DeviceProfiler {

  private static final String TAG = DeviceProfiler.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  public DeviceProfiler() {
  }

  /**
   * If a profile file exists, profile will be loaded and returned.
   * Otherwise a new device profile is generated and saved.
   *
   * @return Loaded or generated device profile
   */
  public DeviceProfile getProfile(File profileFile) throws FileNotFoundException {

    DeviceProfile profile;
    if (profileFile.exists()) {
      profile = readProfile(profileFile);
    }
    else {
      profile = new DeviceProfile(
              getDeviecID(),
              getModel(),
              getMemorySize(),
              getNumCores(),
              0);// TODO: remove field
    }
    return profile;
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
   * Reads getProfile data from given file.
   *
   * @param f File to read getProfile from
   * @return StaticProfile instance
   */
  public static DeviceProfile readProfile(File f) throws FileNotFoundException {
    ObjectInputStream ois = null;
    Object obj = null;
    DeviceProfile profile = null;
    try {
      try {
        ois = new ObjectInputStream(new FileInputStream(f));

        obj = ois.readObject();
      }
      finally {
        if (ois != null) {
          ois.close();
        }
      }

      if (obj instanceof DeviceProfile) {
        profile = (DeviceProfile) obj;
      }
      else {
        LOGGER.severe("invalid profile file");
      }
    }
    catch (StreamCorruptedException ex) {
      LOGGER.severe(ex.toString());
    }
    catch (OptionalDataException ex) {
      Logger.getLogger(DeviceProfiler.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      Logger.getLogger(DeviceProfiler.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ClassNotFoundException ex) {
      Logger.getLogger(DeviceProfiler.class.getName()).log(Level.SEVERE, null, ex);
    }
    return profile;
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
