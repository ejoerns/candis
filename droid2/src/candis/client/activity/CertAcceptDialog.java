package candis.client.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import candis.client.R;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.service.ActivityCommunicator;
import candis.client.service.BackgroundService;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog to accept certificate.
 *
 * @author Enrico Joerns
 */
public class CertAcceptDialog extends DialogFragment {

  private static final String TAG = CertAcceptDialog.class.getName();
  private final X509Certificate mCert;
  private final Messenger mReplyMessenger;

  /**
   *
   * @param activity Activity
   * @param handler Handler
   */
  public CertAcceptDialog(final X509Certificate cert, Messenger messenger) {
    mCert = cert;
    mReplyMessenger = messenger;
  }

  /**
   *
   * @param cahandler
   */
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    try {
      builder.setMessage(
              mCert.getSubjectDN().getName()
              + "\nSHA1: " + ReloadableX509TrustManager.getCertFingerPrint("SHA1", mCert))
              .setTitle("Accept Certificate?")
              .setPositiveButton(R.string.button_accept, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.i(TAG, "User chose to accept certificate");
          // send result ( 1 = accepted)
          try {
            mReplyMessenger.send(Message.obtain(null, ActivityCommunicator.RESULT_CHECK_SERVERCERT, 1, 0));
          }
          catch (RemoteException ex) {
            Log.e(TAG, null, ex);
          }
        }
      })
              .setNegativeButton(R.string.button_reject, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.i(TAG, "User chose to reject certificate");
          // send result ( 0 = refused)
          try {
            mReplyMessenger.send(Message.obtain(null, ActivityCommunicator.RESULT_CHECK_SERVERCERT, 0, 0));
          }
          catch (RemoteException ex) {
            Logger.getLogger(CertAcceptDialog.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      });
    }
    catch (Exception ex) {
      Log.e(TAG, null, ex);
    }
    // Create the AlertDialog object and return it
    return builder.create();
  }
}
