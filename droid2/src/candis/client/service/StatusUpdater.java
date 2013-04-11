package candis.client.service;

import android.app.NotificationManager;
import android.content.Context;
import candis.client.activity.CandisNotification;
import candis.client.comm.ServerConnection;
import candis.common.Message;
import java.security.cert.X509Certificate;

/**
 *
 * @author Enrico Joerns
 */
public class StatusUpdater implements ServerConnection.Receiver {

  private boolean mStatusUpdatesEnabled = true;
  private final NotificationManager mNM;
  private final Context mContext;

  public StatusUpdater(Context context) {
    // get notification manager
    mContext = context;
    mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void OnStatusUpdate(ServerConnection.Status status) {
    if (mStatusUpdatesEnabled) {
      switch (status) {
        case CONNECTED:
          System.out.println("got gonnected status!!!!!");
          mNM.notify(CandisNotification.NOTIFICATION_ID,
                     CandisNotification.getNotification(mContext, "Connected."));
          break;
        case DISCONNECTED:
          System.out.println("got disgonnected status!!!!!");
          mNM.notify(CandisNotification.NOTIFICATION_ID,
                     CandisNotification.getNotification(mContext, "Disconnected."));
          break;
      }
    }
  }

  public void OnNewMessage(Message msg) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void OnCheckServerCert(X509Certificate cert) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
