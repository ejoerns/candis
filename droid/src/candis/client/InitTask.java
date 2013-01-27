package candis.client;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import candis.common.DroidID;
import candis.distributed.droid.StaticProfile;
import candis.system.StaticProfiler;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * InitTask, checks for ID, profile, ?truststore? and generates if not
 * existing.
 */
class InitTask extends AsyncTask<Void, Object, DroidContext> {

  private static final String TAG = "GetIDTask";
  private final Activity mActivity;
  private final File mIDFile;
  private final File mProfileFile;
  private boolean mGenerateID = false;
  private boolean mGeneradeProfile = false;
  private final DroidContext mDroidContext;

  public InitTask(final Activity act, final File idfile, final File profilefile) {
    mActivity = act;
    mIDFile = idfile;
    mProfileFile = profilefile;
    mDroidContext = DroidContext.getInstance();
  }

  @Override
  protected void onPreExecute() {
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
      publishProgress("Generating ID...", Toast.LENGTH_SHORT);
    }
    // load or generate ID
    DroidID id = null;
    if (mGenerateID) {
      id = DroidID.init(mIDFile);
      publishProgress("ID (SHA-1): " + id.toSHA1(), Toast.LENGTH_LONG);
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
    StaticProfile profile;
    if (mGeneradeProfile) {
      profile = new StaticProfiler(mActivity).profile();
      StaticProfiler.writeProfile(mProfileFile, profile);
      publishProgress("Profile generated", Toast.LENGTH_SHORT);
    }
    else {
      profile = StaticProfiler.readProfile(mProfileFile);
    }
    mDroidContext.setID(id);
    System.out.println("InitTask: id now: " + id.toSHA1());
    mDroidContext.setProfile(profile);
    System.out.println("InitTask done...");
    return mDroidContext;
  }

  /**
   * Shows Toasts in UI thread.
   *
   * @param msg String / Integer pait for message and show duration
   */
  @Override
  protected void onProgressUpdate(Object... msg) {
    Toast.makeText(mActivity.getApplicationContext(), (String) msg[0], (Integer) msg[1]).show();
  }
}
