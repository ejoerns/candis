package candis.client.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import candis.client.R;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.service.BackgroundService;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog to accept certificate.
 *
 * @author Enrico Joerns
 */
public class CertAcceptDialog extends DialogFragment {

	private static String TAG = "CertAcceptDialog";
	private final X509Certificate mCert;
	private boolean has_result;
	private final Context mContext;

	/**
	 *
	 * @param activity Activity
	 * @param handler Handler
	 */
	public CertAcceptDialog(final X509Certificate cert, Context context) {
		mCert = cert;
		mContext = context;
	}
	private final AtomicBoolean mIsAccepted = new AtomicBoolean(false);

	/**
	 *
	 * @param cahandler
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		try {
			builder.setMessage(mCert.getSubjectDN().getName() + "\nSHA1: " + ReloadableX509TrustManager.getCertFingerPrint("SHA1", mCert))
							.setTitle("Accept Certificate?")
							.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Log.v(TAG, "User chose to accept certificate");
					has_result = true;
					final Intent intent = new Intent(mContext, BackgroundService.class);
					intent.setAction(BackgroundService.RESULT_CHECK_SERVERCERT);
					intent.putExtra("RESULT", true);
					mContext.startService(intent);
					Log.v(TAG, "Service intent sent...");
				}
			})
							.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Log.v(TAG, "User chose to reject certificate");
					has_result = true;
					final Intent intent = new Intent(mContext, BackgroundService.class);
					intent.setAction(BackgroundService.RESULT_CHECK_SERVERCERT);
					intent.putExtra("RESULT", false);
					mContext.startService(intent);
					Log.v(TAG, "Service intent sent...");
				}
			});
		}
		catch (Exception ex) {
			Logger.getLogger(CertAcceptDialog.class.getName()).log(Level.SEVERE, null, ex);
		}
		// Create the AlertDialog object and return it
		return builder.create();
	}

	public interface Listener {
	}
}