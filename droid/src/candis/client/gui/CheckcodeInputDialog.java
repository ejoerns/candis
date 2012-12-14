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
public class CheckcodeInputDialog extends DialogFragment {

	private static final String TAG = "CheckcodeInputDialog";
	private final FSM mStateMachine;

	public CheckcodeInputDialog(FSM fsm) {
		mStateMachine = fsm;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final EditText input = new EditText(getActivity());
		builder.setView(input);

		final Message msg = Message.obtain();
		try {
			builder.setMessage("Enter check code")
//							.setTitle("Warning")
							.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Log.v(TAG, "Positive clicked with checkcode " + input.getText().toString());
					try {
						mStateMachine.process(
										ClientStateMachine.ClientTrans.CHECKCODE_ENTERED,
										input.getText().toString());
					}
					catch (StateMachineException ex) {
						Logger.getLogger(CheckcodeInputDialog.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			})
							.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Log.v(TAG, "Negative clicked with checkcode " + input.getText().toString());
					// TODO...
				}
			});
		}
		catch (Exception ex) {
			Logger.getLogger(CertAcceptDialog.class.getName()).log(Level.SEVERE, null, ex);
		}
		// Create the AlertDialog object and return it
		return builder.create();
	}
}
