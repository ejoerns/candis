package candis.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import candis.client.comm.CertAcceptHandler;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.ReloadableX509TrustManager;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog to accept certificate.
 *
 * @author Enrico Joerns
 */
public class CertAcceptDialog implements CertAcceptRequest {

	private static String TAG = "CertAcceptDialog";
	private final Activity activity;
	private X509Certificate cert_to_check;
	private boolean has_result;
	// Handler to execute UI stuff
	private final Handler handler;

	/**
	 *
	 * @param activity Activity
	 * @param handler Handler
	 */
	public CertAcceptDialog(final Activity activity, final Handler handler) {
		this.activity = activity;
		this.handler = handler;
	}

	@Override
	public void userCheckAccept(final X509Certificate cert, final CertAcceptHandler cahandler) {
		this.cert_to_check = cert;
		handler.post(new Runnable() {
			public void run() {
				final CertAcceptDialog.Fragment cad_frag = new CertAcceptDialog.Fragment(cahandler);
				cad_frag.show(activity.getFragmentManager(), "mytag");
			}
		});
	}

	/**
	 *
	 * @return Result
	 */
	public boolean hasResult() {
		return has_result;
	}

	/**
	 *
	 */
	private class Fragment extends DialogFragment {

		private CertAcceptHandler cahandler;

		/**
		 * 
		 * @param cahandler 
		 */
		public Fragment(final CertAcceptHandler cahandler) {
			this.cahandler = cahandler;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			try {
				builder.setMessage(cert_to_check.getSubjectDN().getName() + "\nSHA1: " + ReloadableX509TrustManager.getCertFingerPrint("SHA1", cert_to_check))
								.setTitle("Accept Certificate?")
								.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.v(TAG, "User chose to accept certificate");
						has_result = true;
						cahandler.acceptHandler(true);
					}
				})
								.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.v(TAG, "User chose to reject certificate");
						has_result = true;
						cahandler.acceptHandler(false);
					}
				});
			} catch (Exception ex) {
				Logger.getLogger(CertAcceptDialog.class.getName()).log(Level.SEVERE, null, ex);
			}
			// Create the AlertDialog object and return it
			return builder.create();
		}
	}
}
