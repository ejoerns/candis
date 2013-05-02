package candis.client.android.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
//import candis.client.DeviceProfiler;
import candis.client.DroidContext;
import candis.client.android.AndroidDeviceProfiler;
import candis.client.android.AndroidTaskProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InitTask, checks for ID, getProfile, ?truststore? and generates if not
 * existing.
 */
class InitTask extends AsyncTask<Void, Object, Void> {

  private static final String TAG = InitTask.class.getName();
  private final Context mActivity;
  private final File mDataDir;
  private final File mCacheDir;
  private boolean mGenerateID = false;
  private boolean mGeneradeProfile = false;
  private Toast mToast;
  private SharedPreferences mSharedPref;
  private Handler mHandler;
  private InputStream mSettingsInput;

  public InitTask(final Context act, InputStream settings, final File filesDir, final File cacheDir, final Handler handler) {
    mActivity = act;
    mSettingsInput = settings;
    mDataDir = filesDir;
    mCacheDir = cacheDir;
    mHandler = handler;
  }

  @Override
  protected void onPreExecute() {
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
  }

  /**
   * Loads ID from file or creates a new if not exists.
   *
   * @param params
   * @return DroidContext containing loaded or generated data
   */
  @Override
  protected Void doInBackground(Void... params) {
    System.out.println("InitTask started...");
    // check for getProfile file
    try {
      DroidContext.createInstance(mSettingsInput, mDataDir, mCacheDir, new AndroidDeviceProfiler(mActivity), new AndroidTaskProvider(mCacheDir));
    }
    catch (FileNotFoundException ex) {
      Logger.getLogger(InitTask.class.getName()).log(Level.SEVERE, null, ex);
    }

    // TODO: only for debugging...
    publishProgress("Initialization done", Toast.LENGTH_SHORT);

    // Check for initial hostname setting
    checkInitialHostname();

    Log.v(TAG, "InitTask done...");

    return (Void) null;
  }

  /**
   * Shows Toasts in UI thread.
   *
   * @param msg String / Integer pait for message and show duration
   */
  @Override
  protected void onProgressUpdate(Object... msg) {
    if (mToast != null) {
      mToast.cancel();
    }
    mToast = Toast.makeText(mActivity.getApplicationContext(), (String) msg[0], (Integer) msg[1]);
    mToast.show();
  }
  String enteredHostname;

  public void checkInitialHostname() {
    // continue only if hostname is not empty
    if (!mSharedPref.getString("pref_key_servername", "").equals("")) {
      return;
    }
    // Set an EditText view to get user input 

    mHandler.post(new Runnable() {
      public void run() {
        final EditText input = new EditText(mActivity);
//        new AlertDialog.Builder(mActivity)
//                .setTitle("Server Address")
//                .setMessage("Enter address of server here")
//                .setView(input)
//                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//          public void onClick(DialogInterface dialog, int whichButton) {
//            // store in shared preference
//            mSharedPref.edit().putString("pref_key_servername", input.getText().toString()).commit();
//          }
//        })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//          public void onClick(DialogInterface dialog, int whichButton) {
//            // Do nothing.
//          }
//        }).show();
      }
    });

  }
}
