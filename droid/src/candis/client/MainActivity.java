package candis.client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.RandomID;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.common.Utilities;
import candis.system.StaticProfiler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends Activity
				implements OnClickListener {

	private Button startButton;
	private Button stopButton;
	private static final String TAG = "WTF";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Handler mHandler = new Handler();

		startButton = (Button) findViewById(R.id.start_button);
		startButton.setOnClickListener(this);

		stopButton = (Button) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);

		/*
		 * ActivityManager activityManager = (ActivityManager)
		 * getSystemService(ACTIVITY_SERVICE); MemoryInfo memoryInfo = new
		 * ActivityManager.MemoryInfo(); activityManager.getMemoryInfo(memoryInfo);
		 * Log.i(TAG, "Memory: " + memoryInfo.availMem); try { //
		 * Runtime.getRuntime().exec("keytool -genkey -keyalg RSA -alias
		 * \"selfsigned\" -dname \"EmailAddress=personal-freemail@thawte.com\"
		 * -keystore "+ getFilesDir() + "/foobar.bar" + " -storepass \"password\"
		 * -keypass \"changeit\" -validity 360"); Runtime.getRuntime().exec("keytool
		 * -help > " + getFilesDir() + "/foobar.bar"); } catch (IOException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); }
		 */
		//req.testme();

		//		int RND_SIZE = 4096;
		//		SecureRandom random = new SecureRandom();
		//		byte bytes[] = new byte[RND_SIZE / 8];
		//		random.nextBytes(bytes);
		//		
		//		FileInputStream fis = null;
		//		try {
		//			fis = openFileInput("idsequence");
		//		} catch (FileNotFoundException ex) {
		//			FileOutputStream fos = null;
		//			try {
		//				fis.close();
		//				fos = openFileOutput("idsequence", MODE_PRIVATE);
		//			} catch (IOException ex1) {
		//				Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex1);
		//			}
		//			Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		//		}
		//		try {
		//			fos = openFileOutput("idsequence", Context.MODE_PRIVATE);
		//			fos.write(bytes);
		//			Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		//		} catch (IOException ex) {
		//			Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		//		} finally {
		//			if (fos != null) {
		//				try {
		//					fos.close();
		//				} catch (IOException ex) {
		//					Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		//				}
		//			}
		//		}

		RandomID rid;
		String clientid = getFilesDir() + "clientid";
		try {
			rid = RandomID.readFromFile(clientid);
		} catch (FileNotFoundException ex) {
			rid = RandomID.init(clientid);
		}

		Log.v(TAG, "SHA-1: " + Utilities.toSHA1String(rid.getBytes()));

		final Activity act = this;
		new Thread(new Runnable() {
			public void run() {
				StaticProfiler statprof = new StaticProfiler(act, mHandler);
				statprof.benchmark();
			}
		}).start();

		if (true) {
			return;
		}

		new Settings(this.getApplicationContext()).test();

//		System.setProperty("javax.net.ssl.trustStore", "res/raw/.sometruststore");
//		System.setProperty("javax.net.ssl.trustStorePassword", "candist");
/*
		 * KeyStore localTrustStore; try { // load truststore certificate
		 * localTrustStore = KeyStore.getInstance("BKS"); InputStream in =
		 * getResources().openRawResource(R.raw.sometruststore);
		 * localTrustStore.load(in, "candist".toCharArray());
		 *
		 * Log.i(TAG, "Loaded server certificates: " + localTrustStore.size());
		 *
		 * // initialize trust manager factory with the read truststore
		 * TrustManagerFactory trustManagerFactory = null; trustManagerFactory =
		 * TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		 * trustManagerFactory.init(localTrustStore);
		 *
		 * Log.i(TAG, "Starting connection");
		 *
		 * SSLContext context = null; context = SSLContext.getInstance("TLS");
		 * context.init(null, trustManagerFactory.getTrustManagers(), null);
		 * SSLSocketFactory sf = context.getSocketFactory();
		 *
		 * Log.i(TAG, "got SSLSocketFactory");
		 *
		 * Socket socket = sf.createSocket("10.0.2.2", 9999);
		 *
		 * Log.i(TAG, "created Socket"); // Create streams for reading and writing
		 * lines of text // from and to this socket. OutputStream out =
		 * socket.getOutputStream(); //	PrintStream sout = new
		 * PrintStream(socket.getOutputStream()); Log.i(TAG, "PrintStream ready");
		 * InputStream inn = socket.getInputStream(); //	DataInputStream sin = new
		 * DataInputStream(socket.getInputStream()); Log.i(TAG, "DataInputStream
		 * ready"); // Create a stream for reading lines of text from the console //
		 * DataInputStream in = new DataInputStream(System.in);
		 *
		 * // Tell the user that we've connected Log.i(TAG, "Connected to " +
		 * socket.getInetAddress() + ":" + socket.getPort()); inn.close();
		 * out.close();
		 *
		 * } catch (IOException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); Log.e(TAG, ex.toString()); } catch (NoSuchAlgorithmException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); Log.e(TAG, ex.toString()); } catch (CertificateException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); Log.e(TAG, ex.toString()); } catch (KeyStoreException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); Log.e(TAG, ex.toString()); } catch (KeyManagementException ex) {
		 * Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null,
		 * ex); Log.e(TAG, ex.toString()); }
		 */

		SecureConnection sconn = null;
		String tspath = getFilesDir() + "/localtruststore";
		CertAcceptRequest cad = new CertAcceptDialog(this, mHandler);
//		try {
//			sconn = new SecureConnection(
//							"10.0.2.2", 9999,
//							(X509TrustManager) new ReloadableX509TrustManager(tspath, cad));
//		} catch (Exception ex) {
//			Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
//		}

		try {
			new Thread(
							sconn = new SecureConnection(
							"10.0.2.2", 9999,
							(X509TrustManager) new ReloadableX509TrustManager(tspath, cad))).start();
		} catch (Exception ex) {
			Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		}
		// from and to this socket.	
/*
		 try {
		 //			out = socket.getOutputStream();
		 //			Log.i(TAG, "PrintStream ready");
		 //			inn = socket.getInputStream();
		 //			Log.i(TAG, "DataInputStream ready");
		 //			inn.close();
		 //			out.close();

		 //			  byte[] mybytearray = new byte[1024]; InputStream is =
		 //			  socket.getInputStream(); String filename = "example_app-debug.apk";
		 //			  FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
		 //			  BufferedOutputStream bos = new BufferedOutputStream(fos); for (int n;
		 //			  (n = is.read(mybytearray)) != -1; ) bos.write(mybytearray, 0, n);
		 //			  bos.close();
			
		 //			while (!sconn.isConnected()) {Thread.yield();};
		 Log.i(TAG, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			
		 Message req = new Message(Instruction.GET_CERTIFICATE, null);
		 sconn.writeObject(req);
			
		 Message rec = (Message) sconn.readObject();
		 if (rec != null) {
		 Log.v(TAG, "Got answer: " + rec.getRequest());
		 }

		 //			int bytesRead = is.read(mybytearray, 0, mybytearray.length);
		 //			bos.write(mybytearray, 0, bytesRead);
		 } catch (IOException ex) {
		 Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
		 }
		 */
//		final File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);
//		File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE),
//						SECONDARY_DEX_NAME);
//
//		DexClassLoader cl = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
//						optimizedDexOutputPath.getAbsolutePath(),
//						null,
//						getClassLoader());
		/*
		 * try { final String libPath = getFilesDir() + "/example_app-debug.apk";
		 * Log.i(TAG, "lipPath is: " + libPath); final File tmpDir = getDir("dex",
		 * 0); Log.i(TAG, "tmpDir is: " + tmpDir);
		 *
		 * final DexClassLoader classloader = new DexClassLoader( libPath,
		 * tmpDir.getAbsolutePath(), null, this.getClass().getClassLoader());
		 * Log.i(TAG, "DexClassLoader finished"); final Class<?> classToLoad =
		 * (Class<?>) classloader.loadClass("android.example.TestAlgorithm");
		 *
		 * Log.i(TAG, "Class loaded: " + classToLoad.getCanonicalName()); final
		 * Object myInstance = classToLoad.newInstance(); Method[] methods =
		 * classToLoad.getDeclaredMethods(); int funccnt = 0; for (int i = 0; i <
		 * methods.length; i++) { Log.i(TAG, methods[i].toGenericString());
		 * funccnt++; } Log.v(TAG, "Functions: " + funccnt);
		 *
		 * Log.i(TAG, "Loading method..."); final Method doSomething =
		 * classToLoad.getDeclaredMethod("fakultaet", int.class);
		 *
		 * Log.i(TAG, "Method loaded: " + doSomething.toGenericString());
		 * doSomething.invoke(myInstance, 10);
		 *
		 * } catch (Exception e) { e.printStackTrace(); }
		 */



	}

	public void onClick(View v) {
//		EditText nameField = (EditText) findViewById(R.id.name_field);
//		String name = nameField.getText().toString();

		if (v == startButton) {
			Log.d(TAG, "onClick: starting service");
			startService(new Intent(this, MyService.class));
			String feedback = getResources().getString(R.string.start_msg);
			Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
		} else if (v == stopButton) {
			Log.d(TAG, "onClick: stopping service");
			stopService(new Intent(this, MyService.class));
			String feedback = getResources().getString(R.string.stop_msg);
			Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
		}


//		if (name.length() == 0) {
//			new AlertDialog.Builder(this).setMessage(
//							R.string.error_name_missing).setNeutralButton(
//							R.string.error_ok,
//							null).show();
//			return;
//		}

		if (v == startButton || v == stopButton) {
			int resourceId = v == startButton ? R.string.start_msg
							: R.string.stop_msg;



//			TextView greetingField = (TextView) findViewById(R.id.greeting_field);
//			greetingField.setText(feedback);
		}

	}
}
