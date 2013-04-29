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
import candis.client.DeviceProfiler;
//import candis.client.DeviceProfiler;
import candis.client.DroidContext;
import candis.client.android.AndroidDeviceProfiler;
import candis.common.DroidID;
import candis.distributed.droid.DeviceProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InitTask, checks for ID, profile, ?truststore? and generates if not
 * existing.
 */
class InitTask extends AsyncTask<Void, Object, DroidContext> {

  private static final String TAG = InitTask.class.getName();
  private final Context mActivity;
  private final File mIDFile;
  private final File mProfileFile;
  private boolean mGenerateID = false;
  private boolean mGeneradeProfile = false;
  private final DroidContext mDroidContext;
  private Toast mToast;
  private SharedPreferences mSharedPref;
  private Handler mHandler;

  public InitTask(final Context act, final File idfile, final File profilefile, final Handler handler) {
    mActivity = act;
    mIDFile = idfile;
    mProfileFile = profilefile;
    mDroidContext = DroidContext.getInstance();
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
  protected DroidContext doInBackground(Void... params) {
    System.out.println("InitTask started...");
    // check for profile file
    if (!mIDFile.exists()) {
      Log.v(TAG, "ID file " + mIDFile + " does not exist. Will be generated...");
      mGenerateID = true;
//      publishProgress("Generating ID...", Toast.LENGTH_SHORT);
    }

    // load or generate ID
    DroidID id = null;
    if (mGenerateID) {
      try {
        id = DroidID.generate(mIDFile);
        publishProgress("Generated ID (SHA-1): " + id.toSHA1(), Toast.LENGTH_LONG);
      }
      catch (FileNotFoundException ex) {
        Logger.getLogger(InitTask.class.getName()).log(Level.SEVERE, null, ex);
        publishProgress("ID generation failed", Toast.LENGTH_LONG);
      }
    }
    else {
      try {
        id = DroidID.readFromFile(mIDFile);
      }
      catch (FileNotFoundException ex) {
        Log.e(TAG, ex.toString());
      }
    }

    // check for profile file
    if (!mProfileFile.exists()) {
      Log.v(TAG, "Pofile file " + mProfileFile + " does not exist. Run Profiling...");
      mGeneradeProfile = true;
      publishProgress("Starting Profiler...", Toast.LENGTH_SHORT);
    }

    // load or generate profile
    DeviceProfile profile;
    if (mGeneradeProfile) {
      profile = new AndroidDeviceProfiler(mActivity.getApplicationContext()).profile();
      DeviceProfiler.writeProfile(mProfileFile, profile);
//      publishProgress("Profile generated", Toast.LENGTH_SHORT);
    }
    else {
      profile = DeviceProfiler.readProfile(mProfileFile);
    }
    mDroidContext.setID(id);
    Log.v(TAG, "InitTask: id now: " + id.toSHA1());
    mDroidContext.setProfile(profile);

    // TODO: only for debugging...
    publishProgress("Initialization done", Toast.LENGTH_SHORT);

    // Check for initial hostname setting
    checkInitialHostname();

    Log.v(TAG, "InitTask done...");
    return mDroidContext;
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
        new AlertDialog.Builder(mActivity)
                .setTitle("Server Address")
                .setMessage("Enter address of server here")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            // store in shared preference
            mSharedPref.edit().putString("pref_key_servername", input.getText().toString()).commit();
          }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            // Do nothing.
          }
        }).show();
      }
    });

  }
}
