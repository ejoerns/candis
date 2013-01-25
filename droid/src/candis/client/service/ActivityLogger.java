package candis.client.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.Messenger;
import android.os.RemoteException;
import candis.client.JobCenterHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends job center message to logging activity by using intents.
 *
 * @author Enrico Joerns
 */
public class ActivityLogger implements JobCenterHandler {

  private final Messenger mMessenger;
  Intent intent;
  android.os.Message msg;

  public ActivityLogger(Messenger context) {
    mMessenger = context;
    msg = android.os.Message.obtain(null, BackgroundService.JOB_CENTER_HANDLER);
  }

  public void onBinaryReceived(String runnableID) {
    sendMsg(String.format("Task with ID %s received", runnableID));
  }

  public void onInitialParameterReceived(String runnableID) {
    sendMsg(String.format("Initial Paramater for Task with ID %s received", runnableID));
  }

  public void onJobExecutionStart(String runnableID) {
    sendMsg(String.format("Job for Task with ID %s started", runnableID));
  }

  public void onJobExecutionDone(String runnableID) {
    sendMsg(String.format("Job for Task with ID %s stopped", runnableID));
  }

  private void sendMsg(String message) {
    Bundle bundle = new Bundle();
    bundle.putString("Message", message);
    try {
      mMessenger.send(msg);
    }
    catch (RemoteException ex) {
      Logger.getLogger(ActivityLogger.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public void onAction(int action, String runnableID) {
    switch (action) {
      case BINARY_RECEIVED:
        onBinaryReceived(runnableID);
        break;
      case INITIAL_PARAMETER_RECEIVED:
        onInitialParameterReceived(runnableID);
        break;
      case JOB_EXECUTION_DONE:
        onJobExecutionDone(runnableID);
        break;
      case JOB_EXECUTION_START:
        onJobExecutionStart(runnableID);
        break;
    }
  }
}
