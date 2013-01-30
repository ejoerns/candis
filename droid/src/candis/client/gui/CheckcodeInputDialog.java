package candis.client.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.EditText;
import candis.client.R;
import candis.client.service.BackgroundService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class CheckcodeInputDialog extends DialogFragment {

  private static final String TAG = CheckcodeInputDialog.class.getName();
  private final Messenger mMessenger;
  private final String mCheckCodeID;

  public CheckcodeInputDialog(Messenger context, String checkcodeID) {
    mMessenger = context;
    mCheckCodeID = checkcodeID;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    final EditText input = new EditText(getActivity());
    builder.setView(input);

    try {
      builder.setMessage("You are: " + mCheckCodeID)
              .setTitle("Enter check code")
              .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.v(TAG, "Positive clicked with checkcode " + input.getText().toString());
          try {
            // convert string to int and send it
//            Log.e(TAG, "Value seems to be: " + checkcode);
            Message message = Message.obtain(
                    null,
                    BackgroundService.RESULT_SHOW_CHECKCODE);
            Bundle bundle = new Bundle();
            bundle.putString("checkcode", input.getText().toString());
            Log.e(TAG, "checkcode: " + input.getText().toString());
            message.setData(bundle);
            mMessenger.send(message);
          }
          catch (RemoteException ex) {
            Logger.getLogger(CheckcodeInputDialog.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      });
    }
    catch (Exception ex) {
      Logger.getLogger(CheckcodeInputDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    // Create the AlertDialog object and return it
    return builder.create();
  }
}
