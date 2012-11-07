package candis.client.comm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trustmanager that allows to dynamically add new server certificates.
 *
 * @author enrico
 */
public final class ReloadableX509TrustManager
				implements X509TrustManager, CertAcceptHandler {

	private final String tspath;
	private X509TrustManager trustManager;
	private final List<Certificate> tempCertList = new LinkedList<Certificate>();
	private CertAcceptRequest cad;
	private final AtomicBoolean accepted = new AtomicBoolean(false);
	private static final String TAG = "X509";
	private static final Logger logger = Logger.getLogger(TAG);
//	private final Boolean certAccepted = new Boolean(true);

	/**
	 * Creates instance of ReloadableX509TrustManager that accepts server
	 * certificate without user interaction
	 *
	 * @param tspath Path to truststore file (Will be created if not existent)
	 * @throws Exception
	 */
	public ReloadableX509TrustManager(String tspath)
					throws Exception {
		this(tspath, null);
		this.setCertAcceptDialog(null);
		logger.log(Level.INFO, "Constructor called");
	}

	/**
	 * Creates instance of ReloadableX509TrustManager with choosable
	 * ClientAcceptDialog (CAD).
	 *
	 * @param tspath Path to truststore file (Will be created if not existent)
	 * @param cad Implementation of CertAcceptDialog to enable user interaction
	 * @throws Exception
	 */
	public ReloadableX509TrustManager(String tspath, CertAcceptRequest cad) throws Exception {
		this.tspath = tspath;
		this.cad = cad;
		reloadTrustManager();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		logger.log(Level.INFO, "checkClientTrusted()");
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		logger.log(Level.INFO, "checkServerTrusted()");
		try {
			trustManager.checkServerTrusted(chain, authType);
		} catch (java.security.cert.CertificateException cx) {
			addServerCertAndReload(chain[0], true);
			trustManager.checkServerTrusted(chain, authType);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		logger.log(Level.INFO, "getAcceptedIssuers()");
		X509Certificate[] issuers = trustManager.getAcceptedIssuers();
		return issuers;
	}

	/**
	 * Reloads the trust manager from file.
	 *
	 * If the trustmanager file is empty or uninitalized, it will be initialized.
	 *
	 * @throws Exception
	 */
	private void reloadTrustManager() throws Exception {
		logger.log(Level.INFO, "reloadTrustManager()");

		// load keystore from specified cert store (or default)
		KeyStore ts = KeyStore.getInstance("BKS");
		InputStream in;
		// Check if file exists and is not empty
		try {
			in = new FileInputStream(tspath);
			if (in.available() == 0) {
				in.close();
				in = null;
			}
		} catch (FileNotFoundException ex) {
			logger.log(Level.INFO, "Truststore file " + tspath + " not found.");
			in = null;
		}

		// Initialize empty truststore if none available
		if (in == null) {
			OutputStream out = new FileOutputStream(tspath);
			ts.load(null, "candis".toCharArray());
			ts.store(out, "candis".toCharArray());
			out.close();
			in = new FileInputStream(tspath);
			logger.log(Level.INFO, "Initialized empty truststore " + tspath);
		}

		// load truststore
		try {
			ts.load(in, null);
		} finally {
			in.close();
		}

		// add all temporary certs to KeyStore (ts)
		for (Certificate cert : tempCertList) {
			ts.setCertificateEntry(UUID.randomUUID().toString(), cert);
		}

		// initialize a new TMF with the ts we just loaded
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(
						TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);

		// acquire X509 trust manager from factory
		TrustManager tms[] = tmf.getTrustManagers();
		for (int i = 0; i < tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				trustManager = (X509TrustManager) tms[i];
				return;
			}
		}

		throw new NoSuchAlgorithmException(
						"No X509TrustManager in TrustManagerFactory");
	}

	/**
	 * Adds a new certificate either to permantent (truststore) or to temporary
	 * list.
	 *
	 * @param cert Certificate to add
	 * @param permanent If true, certificate will be added permanent, otherwise it
	 * will be added temporary
	 */
	private void addServerCertAndReload(X509Certificate cert, boolean permanent) {
		logger.log(Level.INFO, "addServerCertAndReload()");

		// Call accept dialog if available, otherwise auto-accept
		if (cad != null) {
			cad.userCheckAccept(cert, this);

			synchronized (accepted) {
				try {
					accepted.wait();
				} catch (InterruptedException ex) {
					Logger.getLogger(ReloadableX509TrustManager.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} else {
			accepted.set(true);
		}

		if (accepted.get()) {
			logger.log(Level.INFO, "Certificate ACCEPTED");
		} else {
			logger.log(Level.INFO, "Certificate REJECTED");
		}


		try {
			if (permanent) {
				// import the cert into file trust store
				KeyStore ts = KeyStore.getInstance("BKS");

				// load truststore from file
				FileInputStream cert_istream = new FileInputStream(tspath);
				ts.load(cert_istream, "candis".toCharArray());
				cert_istream.close();

				// add certificate
				ts.setCertificateEntry("candiscert", cert);

				for (Enumeration<String> en = ts.aliases(); en.hasMoreElements();) {
					logger.log(Level.INFO, "Alias found: {0}", en.nextElement());
				}

				// write truststore to file
				FileOutputStream cert_ostream = new FileOutputStream(tspath);
				ts.store(cert_ostream, "candis".toCharArray());
				cert_ostream.close();

				logger.log(Level.FINE, "Added certificate permanently...");
			} else {
				tempCertList.add(cert);
				logger.log(Level.FINE, "Certificate would be added here now (temporary)...");
			}
			reloadTrustManager();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void setCertAcceptDialog(CertAcceptRequest cad) {
		this.cad = cad;
	}

	public boolean hasResult() {
		return true;
	}

	/**
	 *
	 * @param accept
	 */
	public void acceptHandler(boolean accept) {
		synchronized (accepted) {
			accepted.set(accept);
			accepted.notify();
		}
	}

	public static String getCertFingerPrint(String mdAlg, Certificate cert)
					throws Exception {
		byte[] encCertInfo = cert.getEncoded();
		MessageDigest md = MessageDigest.getInstance(mdAlg);
		byte[] digest = md.digest(encCertInfo);
		return toHexString(digest);
	}

	private static String toHexString(byte[] block) {
		StringBuffer buf = new StringBuffer();
		int len = block.length;
		for (int i = 0; i < len; i++) {
			byte2hex(block[i], buf);
			if (i < len - 1) {
				buf.append(":");
			}
		}
		return buf.toString();
	}

	/**
	 * Converts a byte to hex digit and writes to the supplied buffer
	 */
	private static void byte2hex(byte b, StringBuffer buf) {
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'A', 'B', 'C', 'D', 'E', 'F'};
		int high = ((b & 0xf0) >> 4);
		int low = (b & 0x0f);
		buf.append(hexChars[high]);
		buf.append(hexChars[low]);
	}
}
