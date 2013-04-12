package candis.client;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import candis.client.R;
import candis.distributed.droid.StaticProfile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Provides functionality to get static profile information from the device.
 *
 * @author Enrico Joerns
 */
public class DeviceProfiler {

  private static final String TAG = DeviceProfiler.class.getName();
  private final Context mContext;
  private final AtomicBoolean accepted = new AtomicBoolean(false);

  public DeviceProfiler(final Context act) {
    mContext = act;
  }

  /**
   * Runs profiling tests and returns a StaticProfile object including all
   * collected informations.
   *
   * @return
   */
  public StaticProfile profile() {
    return new StaticProfile(
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
  public long getMemorySize() {
    String str1 = "/proc/meminfo";
    String[] arrayOfString;
    long initial_memory = 0;
    try {
      FileReader localFileReader = new FileReader(str1);
      BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
      arrayOfString = localBufferedReader.readLine().split("\\s+");
      //total Memory
      initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
      localBufferedReader.close();
    }
    catch (IOException e) {
      Log.e(TAG, "Failed to read memory size from /proc/meminfo");
    }
    return initial_memory;
  }

  /**
   * Returns the number of cores available in this device, across all
   * processors. Requires: Ability to peruse the filesystem at
   * "/sys/devices/system/cpu"
   *
   * @return The number of cores, or 1 if failed to get result
   */
  public int getNumCores() {
    //Private Class to display only CPU devices in the directory listing
    class CpuFilter implements FileFilter {

      @Override
      public boolean accept(File pathname) {
        //Check if filename is "cpu", followed by a single digit number
        if (Pattern.matches("cpu[0-9]", pathname.getName())) {
          return true;
        }
        return false;
      }
    }
    try {
      //Get directory containing CPU info
      File dir = new File("/sys/devices/system/cpu/");
      //Filter to only list the devices we care about
      File[] files = dir.listFiles(new CpuFilter());
      //Return the number of cores (virtual CPU devices)
      return files.length;
    }
    catch (Exception e) {
      Log.w(TAG, "Failed to get processor number of /sys/devices/sytstem/cpu");
      //Default to return 1 core
      return 1;
    }
  }

  /**
   * Returns the device ID.
   *
   * This might be either the IMEI oder the MAC address
   *
   * @return
   */
  public String getDeviecID() {
    TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    // get IMEI
    String imei = tm.getDeviceId();// depends on device, but may be not available
    // If no IMEI available, get MAC
    if (imei == null) {
      WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
      imei = wifiMgr.getConnectionInfo().getMacAddress();
    }
    // If no MAC available, get android_id
    if (imei == null) {
      imei = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    // imei =  Settings.Secure.ANDROID_ID;// depends on installation, but may be not unique
    Log.v(TAG, "IMEI/MAC: " + imei);
    return imei;
  }

  /**
   * Returns the device model.
   *
   * Might be used to lookup performance data from a table
   *
   * @return Model of device
   */
  public String getModel() {
    String model = android.os.Build.MODEL;
    Log.v(TAG, "Model: " + model);
    return model;
  }

  /**
   * Reads profile data from given file.
   *
   * @param f File to read profile from
   * @return StaticProfile instance
   */
  public static StaticProfile readProfile(File f) {
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new FileInputStream(f));

      Object obj = ois.readObject();

      if (obj instanceof StaticProfile) {
        return (StaticProfile) obj;
      }
      else {
        Log.e(TAG, "invalid profile file");
        return null;
      }
    }
    catch (StreamCorruptedException ex) {
      Log.e(TAG, ex.toString());
    }
    catch (FileNotFoundException ex) {
      Log.e(TAG, "Profile file " + f + " not found!");
    }
    catch (IOException ex) {
      Log.e(TAG, ex.toString());
    }
    catch (ClassNotFoundException ex) {
      Log.e(TAG, ex.toString());
    }
    finally {
      if (ois != null) {
        try {
          ois.close();
        }
        catch (IOException ex) {
          Log.e(TAG, ex.toString());
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
  public static void writeProfile(File f, StaticProfile p) {
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(f));
      oos.writeObject(p);
    }
    catch (FileNotFoundException ex) {
      Log.e(TAG, ex.toString());
    }
    catch (IOException ex) {
      Log.e(TAG, ex.toString());
    }
    finally {
      if (oos != null) {
        try {
          oos.close();
        }
        catch (IOException ex) {
          Log.e(TAG, ex.toString());
        }
      }
    }
  }
}
