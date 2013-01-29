package candis.client.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import candis.client.JobCenterHandler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends job center message to logging activity by using intents.
 *
 * @author Enrico Joerns
 */
public class ActivityLogger implements JobCenterHandler {

  public static final String LOGFILE = "log.txt";
  private FileOutputStream mFileOutputStream;
  private final Messenger mMessenger;
  private final Context mContext;
  private final android.os.Message msg;

  public ActivityLogger(Messenger messenger, Context context) {
    mMessenger = messenger;
    mContext = context;
    msg = android.os.Message.obtain(null, BackgroundService.LOG_MESSAGE);
  }

  public void onBinaryReceived(String runnableID) {
    putLog(String.format("Task with ID %s received", runnableID));
  }

  public void onInitialParameterReceived(String runnableID) {
    putLog(String.format("Initial Paramater for Task with ID %s received", runnableID));
  }

  public void onJobExecutionStart(String runnableID) {
    putLog(String.format("Job for Task with ID %s started", runnableID));
  }

  public void onJobExecutionDone(String runnableID) {
    putLog(String.format("Job for Task with ID %s stopped", runnableID));
  }

  private void putLog(String message) {
    Bundle bundle = new Bundle();
    bundle.putString("Message", message);
    try {
      mMessenger.send(msg);
    }
    catch (RemoteException ex) {
      Logger.getLogger(ActivityLogger.class.getName()).log(Level.SEVERE, null, ex);
    }
    // write to logfile
    try {
      mFileOutputStream = mContext.openFileOutput(LOGFILE, Context.MODE_APPEND);
      mFileOutputStream.write(message.getBytes());
      mFileOutputStream.write("\n".getBytes());
      mFileOutputStream.close();
    }
    catch (FileNotFoundException ex) {
      Log.e(getClass().getName(), "", ex);
    }
    catch (IOException ex) {
      Log.e(getClass().getName(), "", ex);
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
