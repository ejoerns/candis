/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client.service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.MainActivity;
import candis.client.comm.CertAcceptRequestHandler;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.common.fsm.FSM;
import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Enrico Joerns
 */
class ConnectTask extends AsyncTask<Object, Object, SecureConnection> implements CertAcceptRequestHandler {

	private static final String TAG = "ConnectTask";
	Context mContext;
	private final File tstore;
	CertAcceptRequestHandler cad;
	private X509TrustManager tmanager;
	private SecureConnection mSConn = null;
	private CommRequestBroker mCRB;
	private FSM mFSM;
	private final BackgroundService outer;
	private AtomicBoolean cert_check_result;
	private DroidContext mDroidContext;

	public ConnectTask(final AtomicBoolean bool, final DroidContext dcontext, final Context context, final File ts, final BackgroundService outer) {
		cert_check_result = bool;
		mDroidContext = dcontext;
		this.outer = outer;
		mContext = context;
		tstore = ts;
	}

	@Override
	protected void onPreExecute() {
		Toast.makeText(mContext.getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected SecureConnection doInBackground(Object... params) {
		try {
			tmanager = new ReloadableX509TrustManager(tstore, this);
		}
		catch (Exception ex) {
			Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
		}
		SecureConnection sc = new SecureConnection(tmanager);
		sc.connect((String) params[0], ((Integer) params[1]).intValue());
		try {
			mSConn = sc;
			Log.i(TAG, "Starting CommRequestBroker");
			mFSM = new ClientStateMachine(mSConn, mDroidContext.getID(), mDroidContext.getProfile(), null, null); // TODO: check handler usage
			// TODO: check handler usage
			// TODO: check handler usage
			mCRB = new CommRequestBroker(mSConn.getInputStream(), mFSM);
			new Thread(mCRB).start();
			Log.i("Droid", "[DONE]");
		}
		catch (StreamCorruptedException ex) {
			Log.e(TAG, ex.toString());
		}
		catch (IOException ex) {
			Log.e(TAG, ex.toString());
		}
		return sc;
	}

	@Override
	protected void onProgressUpdate(Object... arg) {
	}

	@Override
	protected void onPostExecute(SecureConnection args) {
		Log.w(TAG, "CONNECTED!!!!");
	}

	public boolean hasResult() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean userCheckAccept(X509Certificate cert) {
		Intent newintent = new Intent(mContext, MainActivity.class);
		newintent.setAction(BackgroundService.CHECK_SERVERCERT).putExtra("X509Certificate", cert).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(newintent);
		synchronized (cert_check_result) {
			try {
				System.out.println("cert_check_result.wait()");
				cert_check_result.wait();
				System.out.println("cert_check_result.wait() done");
			}
			catch (InterruptedException ex) {
				Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return cert_check_result.get(); // TODO...
		// TODO...
		// TODO...
	}
}
