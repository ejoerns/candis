package candis.client;

import android.app.Application;
import android.content.Context;

/**
 *
 * @author Enrico Joerns
 */
public class CandisApp extends Application {

  private static Context context;

  @Override
  public void onCreate() {
    super.onCreate();
    CandisApp.context = getApplicationContext();
  }

  public static Context getAppContext() {
    return CandisApp.context;
  }
}