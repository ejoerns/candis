package candis.client.android;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import candis.client.DeviceProfiler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Provides functionality to get static profile information from the device.
 *
 * @author Enrico Joerns
 */
public class AndroidDeviceProfiler extends DeviceProfiler {

  private static final String TAG = AndroidDeviceProfiler.class.getName();
  private final Context mContext;
  private final AtomicBoolean accepted = new AtomicBoolean(false);

  public AndroidDeviceProfiler(final Context act) {
    super();
    mContext = act;
  }

  /**
   * Reads the entire memory size of the device
   *
   * @return Size in [bytes]
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
  public String getModel() {
    String model = android.os.Build.MODEL;
    Log.v(TAG, "Model: " + model);
    return model;
  }
}
