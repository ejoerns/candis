package candis.client.service;

import android.app.NotificationManager;
import android.content.Context;
import candis.client.JobCenterHandler;
import candis.client.activity.CandisNotification;
import candis.client.comm.ServerConnection;
import candis.common.Message;
import candis.distributed.DistributedJobResult;
import java.security.cert.X509Certificate;

/**
 *
 * @author Enrico Joerns
 */
public class StatusUpdater implements ServerConnection.Receiver, JobCenterHandler {

  private boolean mStatusUpdatesEnabled = true;
  private final NotificationManager mNM;
  private final Context mContext;

  public StatusUpdater(Context context) {
    // get notification manager
    mContext = context;
    mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void notify(String msg) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, msg));
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

  //- JobCenter handler stuff
  public void onAction(int action, String runnableID) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onBinaryReceived(String runnableID) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onInitialParameterReceived(String runnableID) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onJobExecutionStart(String runnableID) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void onJobExecutionDone(String runnableID, DistributedJobResult result, long exectime) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
