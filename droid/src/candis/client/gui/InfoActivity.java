package candis.client.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import candis.client.CurrentSystemStatus;
import candis.client.DroidContext;
import candis.client.R;
import candis.client.gui.settings.SettingsActivity;

/**
 * Shows some informations about client.
 *
 * I.e. ID, profile, etc.
 *
 * @todo...
 *
 * @author Enrico Joerns
 */
public class InfoActivity extends Activity {

  private TextView mServerInfo;
  private TextView mIDInfo;
  private TextView mProfileModel;
  private TextView mProfileID;
  private TextView mProfileCPUs;
  private TextView mProfileMemory;
  private TextView mProfileBenchmark;
  private CurrentSystemStatus mCurrentSystemStatus;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.info);
//    mCurrentSystemStatus = CurrentSystemStatus.getStatus();
//    System.out.println("getStatus() @ InfoActivity: " + CurrentSystemStatus.getStatus());
    mServerInfo = ((TextView) findViewById(R.id.info_server_id));//.setText("35:EC:D9:90:BC:A8:77:92:81:E0:E0:06:7B:9C:25:6A:92:51:9A:14");
    mIDInfo = ((TextView) findViewById(R.id.info_id_id));
    mProfileModel = ((TextView) findViewById(R.id.info_profile_model_value));
    mProfileID = ((TextView) findViewById(R.id.info_profile_deviceid_value));
    mProfileCPUs = ((TextView) findViewById(R.id.info_profile_cpus_value));
    mProfileMemory = ((TextView) findViewById(R.id.info_profile_memory_value));
    mProfileBenchmark = ((TextView) findViewById(R.id.info_profile_benchmark_value));
  }

  @Override
  public void onResume() {
    Log.i("InfoActivity", "onResume()");
    super.onResume();
    // load droid context and shared preference
    DroidContext droidContext = DroidContext.getInstance();
    SharedPreferences mSharedPrefs = getSharedPreferences(
            CurrentSystemStatus.CURRENT_SYSTEM_STATUS,
            Context.MODE_PRIVATE);
    //
    mServerInfo.setText(String.format(
            "%s:%d",
            mSharedPrefs.getString(CurrentSystemStatus.SERVER_NAME, "unknown"),
            mSharedPrefs.getInt(CurrentSystemStatus.SERVER_PORT, 0)));
    mIDInfo.setText(droidContext.getID().toSHA1());
    mProfileModel.setText(droidContext.getProfile().model);
    mProfileID.setText(droidContext.getProfile().id);
    mProfileCPUs.setText(String.valueOf(droidContext.getProfile().processors));
    mProfileMemory.setText(String.valueOf(droidContext.getProfile().memoryMB));
    mProfileBenchmark.setText(String.valueOf(droidContext.getProfile().benchmark));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent newintent;
    switch (item.getItemId()) {
//			case android.R.id.home:
//				finish();
//				return true;
      case R.id.menu_info:
        finish();
        return true;
      case R.id.menu_settings:
        newintent = new Intent(this, SettingsActivity.class);
        newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(newintent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings, menu);
    return true;
  }
}
