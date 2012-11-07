package candis.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import candis.client.comm.CertAcceptHandler;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.ReloadableX509TrustManager;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author enrico
 */
public class CertAcceptDialog implements CertAcceptRequest {

	private boolean accept = false;
	private boolean clicked = false;
	final private Activity activity;
	AlertDialog.Builder builder;
	private X509Certificate cert_to_check;
	private boolean accepted = false;
	private boolean has_result;
	private Handler handler;

	public CertAcceptDialog(Activity activity, Handler handler) {
		this.activity = activity;
		this.handler = handler;
	}

	public void userCheckAccept(final X509Certificate cert, final CertAcceptHandler cahandler) {
		Log.i("CAD", "userCheckAccept()");
		this.cert_to_check = cert;
		handler.post(new Runnable() {
			public void run() {
				final CertAcceptDialog.Fragment cad_frag = new CertAcceptDialog.Fragment(cahandler);
				cad_frag.show(activity.getFragmentManager(), "mytag");
			}
		});
		Log.i("CAD", "userCheckAccept() END");
	}

	public boolean hasResult() {
		return has_result;
	}

	private class Fragment extends DialogFragment {

		private CertAcceptHandler cahandler;
		Message msg;

		public Fragment(CertAcceptHandler cahandler) {
			this.cahandler = cahandler;
			msg = Message.obtain();
			msg.arg1 = -1;
		}

		/**
		 *
		 * @param savedInstanceState
		 * @return
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			builder = new AlertDialog.Builder(getActivity());
			try {
				builder.setMessage(cert_to_check.getSubjectDN().getName() + "\nSHA1: " + ReloadableX509TrustManager.getCertFingerPrint("SHA1", cert_to_check))
								.setTitle("Accept Certificate?")
								.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						has_result = true;
						accepted = true;
						msg.arg1 = 1;
						cahandler.acceptHandler(false);
					}
				})
								.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						has_result = true;
						accepted = false;
						msg.arg1 = 0;
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
