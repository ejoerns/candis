package candis.client.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;
import java.util.LinkedList;
import java.util.List;

/**
 * Receives System status Broadcasts and checks with settings.
 *
 * @author Enrico Joerns
 */
public class SystemStatusReceiver extends BroadcastReceiver {

  private static final String TAG = SystemStatusReceiver.class.getName();
  private List<Listener> listeners = new LinkedList<Listener>();
  // network state
  public static boolean network_connected;
  public static boolean wifi_active;
  // power state
  public static boolean charge_usb;
  public static boolean charge_ac;
  public static boolean charging;
  public static float battery_level;

  @Override
  public void onReceive(Context context, Intent intent) {

    Log.d(TAG, "Intent is: " + intent.getAction());

    if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
      // get charging state
      int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      charging = status == BatteryManager.BATTERY_STATUS_CHARGING
              || status == BatteryManager.BATTERY_STATUS_FULL;
      Log.w(TAG, "Charging: " + charging);

      // get connected adapters (not used)
      int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
      charge_usb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
      charge_ac = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
      Log.w(TAG, "USB charge: " + charge_usb);
      Log.w(TAG, "AC charge: " + charge_ac);

      // get battery level
      int rawlevel = intent.getIntExtra("level", -1);
      float scale = (float) intent.getIntExtra("scale", -1);
      battery_level = (float) -1.0;
      if (rawlevel >= 0 && scale > 0) {
        battery_level = rawlevel / scale;
      }
      Log.w(TAG, "Level: " + battery_level);
    }
    else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
      NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
      network_connected = networkInfo.isConnected();
      if (networkInfo.isConnected()) {
        // Wifi is connected
        wifi_active = true;
        Log.d(TAG, "Wifi is connected: " + String.valueOf(networkInfo));
      }
    }
    else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
      NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
      network_connected = networkInfo.isConnected();
      if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !networkInfo.isConnected()) {
        // Wifi is disconnected
        wifi_active = false;
        Log.d(TAG, "Wifi is disconnected: " + String.valueOf(networkInfo));
      }
    }
    else {
      Log.w(TAG, "Unknown Intent" + intent.getAction());
    }
    Log.d(TAG, "Network is : " + (network_connected ? "connected" : "unconnected"));

    for (Listener l : listeners) {
      l.OnSystemStatusUpdate();
    }

  }

  public void addListener(Listener l) {
    if (l != null) {
      listeners.add(l);
    }
  }

  /**
   * Interface must be implemented by listeners.
   */
  public interface Listener {

    /**
     * Called if system status is upated.
     *
     * If match is positive, system status matches with the rules defined in
     * preferences.
     */
    public abstract void OnSystemStatusUpdate();
  }
}
