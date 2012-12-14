package candis.client.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import candis.client.ClientStateMachine;
import candis.client.R;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ErrorMessageDialog extends DialogFragment {

	private static final String TAG = "CheckcodeInputDialog";
	private final String mMessage;

	public ErrorMessageDialog(String msg) {
		mMessage = msg;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final Message msg = Message.obtain();
		builder.setMessage(mMessage)
						.setTitle("Error")
						.setPositiveButton(R.string.error_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Log.v(TAG, "Ok clicked");
			}
		});
		// Create the AlertDialog object and return it
		return builder.create();
	}
}
