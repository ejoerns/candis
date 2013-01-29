package candis.client.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;
import candis.client.CurrentSystemStatus;

/**
 * Receives Battery Broadcasts.
 *
 * @author Enrico Joerns
 */
public class PowerConnectionReceiver extends BroadcastReceiver {

  SharedPreferences.Editor mSharedPrefEditor;

  public PowerConnectionReceiver(Context context) {
    mSharedPrefEditor = context.getSharedPreferences(CurrentSystemStatus.CURRENT_SYSTEM_STATUS, Context.MODE_PRIVATE).edit();
  }

  @Override
  public void onReceive(Context context, Intent batteryIntent) {
    // get charging state
    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
            || status == BatteryManager.BATTERY_STATUS_FULL;
    Log.w("PowerConnectionReceiver", "Charging: " + charging);

    // get connected adapters (not used)
    int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    Log.w("PowerConnectionReceiver", "USB charge: " + usbCharge);
    Log.w("PowerConnectionReceiver", "AC charge: " + acCharge);

    // get battery level
    int rawlevel = batteryIntent.getIntExtra("level", -1);
    float scale = (float) batteryIntent.getIntExtra("scale", -1);
    float level = (float) -1.0;
    if (rawlevel >= 0 && scale > 0) {
      level = rawlevel / scale;
    }
    Log.w("PowerConnectionReceiver", "Level: " + level);

    // store and commit data
    mSharedPrefEditor
            .putBoolean(CurrentSystemStatus.POWER_CHARGING, charging)
            .putFloat(CurrentSystemStatus.POWER_LEVEL, level)
            .commit();

  }
}
