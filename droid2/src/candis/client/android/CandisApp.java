package candis.client.android;

import android.app.Application;
import android.content.Context;

/**
 * Allows acces to Application Context.
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