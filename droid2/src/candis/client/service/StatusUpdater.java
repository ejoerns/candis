package candis.client.service;

import android.app.NotificationManager;
import android.content.Context;
import candis.client.JobCenterHandler;
import candis.client.activity.CandisNotification;
import candis.client.comm.ServerConnection;
import candis.common.Message;
import candis.distributed.DistributedJobResult;

/**
 *
 * @author Enrico Joerns
 */
public class StatusUpdater implements ServerConnection.Receiver, JobCenterHandler {

  private boolean mNotificationsEnabled = true;
  private final NotificationManager mNM;
  private final Context mContext;

  public StatusUpdater(Context context) {
    // get notification manager
    mContext = context;
    mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void setEnableNotifications(boolean enable) {
    mNotificationsEnabled = enable;
  }

  public void notify(String msg) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, msg));
  }

  public void OnStatusUpdate(ServerConnection.Status status) {
    if (mNotificationsEnabled) {
      switch (status) {
        case CONNECTED:
          mNM.notify(CandisNotification.NOTIFICATION_ID,
                     CandisNotification.getNotification(mContext, "Connected."));
          break;
        case DISCONNECTED:
          mNM.notify(CandisNotification.NOTIFICATION_ID,
                     CandisNotification.getNotification(mContext, "Disconnected."));
          break;
      }
    }
  }

  public void OnNewMessage(Message msg) {
  }

  //- JobCenter handler stuff
  public void onAction(int action, String runnableID) {
  }

  public void onBinaryReceived(String runnableID) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, "Runnable received " + runnableID));
  }

  public void onInitialParameterReceived(String runnableID) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, "Initial param received " + runnableID));
  }

  public void onJobExecutionStart(String runnableID, String jobID) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, "Started Job " + jobID));
  }

  public void onJobExecutionDone(String runnableID, String jobID, DistributedJobResult[] result, long exectime) {
    mNM.notify(CandisNotification.NOTIFICATION_ID,
               CandisNotification.getNotification(mContext, "Finished Job " + exectime + "ms"));
  }

  public void onBinaryRequired(String taskID) {
  }
}
