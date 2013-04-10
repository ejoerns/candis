package candis.client.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.Log;
import java.util.LinkedList;
import java.util.List;

/**
 * Receives System status Broadcasts and checks with settings.
 *
 * @author Enrico Joerns
 */
public class SystemStatusController extends BroadcastReceiver {

  private static final String TAG = SystemStatusController.class.getName();
  private List<Listener> listeners = new LinkedList<Listener>();
  private NetworkInfo.State mMobile;
  private NetworkInfo.State mWifi;
  private boolean mUSBCharge;
  private boolean mACCharge;
  private boolean mCharging;
  private float mLevel;

  @Override
  public void onReceive(Context context, Intent intent) {

    Log.d(TAG, "Intent is: " + intent.getAction());

    if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
      // get charging state
      int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      mCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
              || status == BatteryManager.BATTERY_STATUS_FULL;
      Log.w(TAG, "Charging: " + mCharging);

      // get connected adapters (not used)
      int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
      mUSBCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
      mACCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
      Log.w(TAG, "USB charge: " + mUSBCharge);
      Log.w(TAG, "AC charge: " + mACCharge);

      // get battery level
      int rawlevel = intent.getIntExtra("level", -1);
      float scale = (float) intent.getIntExtra("scale", -1);
      mLevel = (float) -1.0;
      if (rawlevel >= 0 && scale > 0) {
        mLevel = rawlevel / scale;
      }
      Log.w(TAG, "Level: " + mLevel);
    }
    else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
      ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      // mobile
      mMobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
      // wifi
      mWifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
    }
    else {
      Log.w(TAG, "Unknown Intent" + intent.getAction());
    }

    for (Listener l : listeners) {
      l.OnSystemStatusUpdate(matchRule());
    }

  }

  private boolean matchRule() {
    // check battery
    // TODO...

    // check network
    // TODO...
    if (mMobile == NetworkInfo.State.CONNECTED || mMobile == NetworkInfo.State.CONNECTING) {
      //mobile
    }
    else if (mWifi == NetworkInfo.State.CONNECTED || mWifi == NetworkInfo.State.CONNECTING) {
      //wifi
    }
    return true;
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
    public abstract void OnSystemStatusUpdate(boolean match);
  }
}
