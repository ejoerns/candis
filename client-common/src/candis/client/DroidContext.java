package candis.client;

import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.ServerConnection;
import candis.common.DroidID;
import candis.common.Settings;
import candis.distributed.droid.DeviceProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds data associated with the droid.
 *
 * Implemented as singleton.
 *
 * @author Enrico Joerns
 */
public class DroidContext {

  private DroidID mDroidID;
  private DeviceProfile mProfile;
  private ReloadableX509TrustManager mTrustManager;
  private JobCenter mJobCenter;
  private ClientFSM mClientFSM;
  private ServerConnection mConnection;
  private static DroidContext mInstance = null;
  private File mFilesDir;
  private File mCacheDir;

  protected DroidContext(InputStream settings, File filesDir, File cacheDir, DeviceProfiler profiler, TaskProvider provier) throws FileNotFoundException {
    Settings.load(settings);
    mFilesDir = filesDir;
    mCacheDir = cacheDir;
    mDroidID = DroidID.getInstance(new File(mFilesDir, Settings.getString("idstore")));
    mProfile = profiler.getProfile(new File(mFilesDir, Settings.getString("profilestore")));
    try {
      mTrustManager = new ReloadableX509TrustManager(new File(mFilesDir, Settings.getString("truststore")));
    }
    catch (Exception ex) {
      Logger.getLogger(DroidContext.class.getName()).log(Level.SEVERE, null, ex);
    }
    mJobCenter = new JobCenter(mFilesDir, mCacheDir, provier);
    mConnection = new ServerConnection(mTrustManager);
    mClientFSM = new ClientFSM(mJobCenter, mConnection);
  }

  public static DroidContext getInstance() {
    return mInstance;
  }

  public static DroidContext createInstance(
          InputStream settings,
          File filesDir,
          File cacheDir,
          DeviceProfiler profiler,
          TaskProvider provider) throws FileNotFoundException {
    if (mInstance == null) {
      mInstance = new DroidContext(settings, filesDir, cacheDir, profiler, provider);
    }
    return mInstance;
  }

  public DroidID getID() {
    return mDroidID;
  }

  public DeviceProfile getProfile() {
    return mProfile;
  }

  public ReloadableX509TrustManager getTrustManager() {
    return mTrustManager;
  }

  public JobCenter getJobCenter() {
    return mJobCenter;
  }

  public ServerConnection getConnection() {
    return mConnection;
  }

  public ClientFSM getClientFSM() {
    return mClientFSM;
  }
}
