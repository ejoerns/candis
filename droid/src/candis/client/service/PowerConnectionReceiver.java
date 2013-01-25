package candis.client.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import candis.client.CurrentSystemStatus;

/**
 * Receives Battery Broadcasts.
 *
 * @author Enrico Joerns
 */
public class PowerConnectionReceiver extends BroadcastReceiver {

//  private CurrentSystemStatus mSystemStatus = new CurrentSystemStatus();

  public PowerConnectionReceiver(CurrentSystemStatus systemStatus) {
//    mSystemStatus = systemStatus;
  }

  @Override
  public void onReceive(Context context, Intent batteryIntent) {
    // get charging state
    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//    mSystemStatus.charging = status == BatteryManager.BATTERY_STATUS_CHARGING
//            || status == BatteryManager.BATTERY_STATUS_FULL;
//    Log.w("PowerConnectionReceiver", "Charging: " + mSystemStatus.charging);

    // get connected adapters (not used)
    int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    Log.w("PowerConnectionReceiver", "USB charge: " + usbCharge);
    Log.w("PowerConnectionReceiver", "AC charge: " + acCharge);

    // get battery level
    int rawlevel = batteryIntent.getIntExtra("level", -1);
    double scale = batteryIntent.getIntExtra("scale", -1);
//    mSystemStatus.chargingState = -1;
    if (rawlevel >= 0 && scale > 0) {
//      mSystemStatus.chargingState = rawlevel / scale;
    }

//    Log.w("PowerConnectionReceiver", "Level: " + mSystemStatus.chargingState);
  }
}
