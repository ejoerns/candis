/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client.comm;

import android.util.Log;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * SSL/TLS based secure connection.
 *
 *
 *
 * @author enrico
 */
public class SecureConnection implements Runnable {

	KeyStore localTrustStore;
	KeyStore locatKeyStore;
//	TrustManagerFactory trustManagerFactory;
//	SSLContext context;
	Socket socket = null;
	private static final String TAG = "SConn";
	private String host;
	private int port;
	private InputStream keystore_R;
	private boolean connected;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	X509TrustManager tstore;
	private boolean is_onnected;

	/**
	 *
	 * @param host Host address to connect with
	 * @param port port number to connect to
	 * @param truststore_R truststore file to be used
	 */
	public SecureConnection(String host, int port, X509TrustManager tstore) {
		this.host = host;
		this.port = port;
		this.tstore = tstore;
	}

	public void run() {
		connect();
	}

	/**
	 * Creates a socket.
	 *
	 * @return Socket if successfull or null if failed.
	 */
	public void connect() {

		if (connected) {
			Log.w(TAG, "Already connected");
			return;
		}
		connected = true;

//		try {
//			localTrustStore = KeyStore.getInstance("BKS");
//		} catch (KeyStoreException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			Log.e(TAG, "Creating KeyStore failed!");
//			return null;
//		}
//
//		InputStream in;
//		OutputStream out;
//		
//		try {
//			in = new FileInputStream(tspath);
//			out = new FileOutputStream(tspath);
//		} catch (FileNotFoundException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			return null;
//		}
//		
//		try {
//			if (in.available() == 0) {
//				try {
//					localTrustStore.store(out, "candis".toCharArray());
//				} catch (KeyStoreException ex) {
//					Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//				}
//			}
//			Log.i(TAG, "Available: " + in.available());
//			localTrustStore.load(in, "candist".toCharArray());
//			Log.i(TAG, "Truststore loaded");
//			in.reset();
//			Log.i(TAG, "Truststore reseted");
//		} catch (IOException ex) {
////			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			Log.e(TAG, "Failed loading certificates! [IOException]");
//			return null;
//		} catch (NoSuchAlgorithmException ex) {
////			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			Log.e(TAG, "Kein Rhythmus! [NoSuchAlgorithmException]");
//			return null;
//		} catch (CertificateException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			Log.e(TAG, "Certificate failure!");
//			return null;
//		}
//		try {
//			Log.d(TAG, "Loaded server certificates: " + localTrustStore.size());
//		} catch (KeyStoreException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//		}
//
//		// initialize trust manager factory with the read truststore
//		TrustManagerFactory trustManagerFactory = null;
//		try {
//			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//			trustManagerFactory.init(localTrustStore);
//		} catch (NoSuchAlgorithmException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			return null;
//		} catch (KeyStoreException ex) {
//			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
//			return null;
//		}

		Log.v(TAG, "Starting connection");

		SSLContext context = null;
		try {
			context = SSLContext.getInstance("TLS");
			//			context.init(null, trustManagerFactory.getTrustManagers(), null);
			context.init(null, new TrustManager[]{tstore}, null);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
			return;
		} catch (KeyManagementException ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
			return;
		} catch (Exception ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
		}
		SSLSocketFactory sf = context.getSocketFactory();

		Log.v(TAG, "Got SSLSocketFactory");
		try {
			socket = sf.createSocket(host, port);
		} catch (UnknownHostException ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
		}

		Log.i(TAG, "Connected to " + socket.getInetAddress()
						+ ":" + socket.getPort());

		Log.v(TAG, "Creating input/output streams");
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException ex) {
			Log.e(TAG, "Failed creating input/output streams");
		}

		is_onnected = true;
		Log.v("X509", "Now Connected!");
	}

	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ex) {
			Log.e(TAG, "Failed to close socket");
		}
	}

	public Socket getSocket() {
		return socket;
	}

	public Object readObject() throws IOException {
		Object rec = null;

		if (ois.available() > 0) {
			try {
				rec = ois.readObject();
			} catch (OptionalDataException ex) {
				Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			Log.v(TAG, "No data");
		}

		return rec;
	}

	public void writeObject(Object data) throws IOException {
		oos.writeObject(data);
	}

	public boolean isConnected() {
//		Log.v("X509", "Called isConnected()");
		return is_onnected;
	}
}
