package candis.client.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import candis.client.R;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class KillProcessesDialogFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final Message msg = Message.obtain();
		try {
			builder.setMessage("Will terminate all current processes")
							.setTitle("Warning")
							.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
//						Log.v(TAG, "User chose to accept");
//						synchronized (accepted) {
//							accepted.set(true);
//							accepted.notify();
//						}
				}
			})
							.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
//						Log.v(TAG, "User chose to reject");
//						synchronized (accepted) {
//							accepted.set(false);
//							accepted.notify();
//						}
				}
			});
		}
		catch (Exception ex) {
			Logger.getLogger(KillProcessesDialogFragment.class.getName()).log(Level.SEVERE, null, ex);
		}
		// Create the AlertDialog object and return it
		return builder.create();
	}
}