package candis.client.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
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

	private static final String TAG = "CheckcodeInputDialog";
	private final Context mContext;

	public CheckcodeInputDialog(Context context) {
		mContext = context;
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
					final Intent intent = new Intent(mContext, BackgroundService.class);
					intent.setAction(BackgroundService.RESULT_SHOW_CHECKCODE)
									.putExtra("RESULT", input.getText().toString());
					mContext.startService(intent);
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
