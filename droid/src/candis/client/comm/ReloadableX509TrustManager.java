package candis.client.comm;

import candis.common.Utilities;
import java.io.File;
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
 * @author Enrico Joerns
 */
public final class ReloadableX509TrustManager
				implements X509TrustManager, CertAcceptHandler {

	private static final String TAG = "X509";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	/// Path to truststore
	private final File mTSFile;
	private X509TrustManager mTrustManager;
	/// List for temporary certificates
	private final List<Certificate> mTempCertList = new LinkedList<Certificate>();
	private CertAcceptRequest mCAR;
	/// Boolean for synchronization
	private final AtomicBoolean accepted = new AtomicBoolean(false);

	/**
	 * Creates instance of ReloadableX509TrustManager that accepts server
	 * certificate without user interaction
	 *
	 * @param tspath Path to truststore file (Will be created if not existent)
	 * @throws Exception
	 */
	public ReloadableX509TrustManager(final String tspath)
					throws Exception {
		this(tspath, null);
	}

	/**
	 * Creates instance of ReloadableX509TrustManager with choosable
	 * ClientAcceptDialog (CAD).
	 *
	 * @param tsfile Path to truststore file (Will be created if not existent)
	 * @param cad Implementation of CertAcceptDialog to enable user interaction
	 * @throws Exception
	 */
	public ReloadableX509TrustManager(final File tsfile, final CertAcceptRequest cad) throws Exception {
		this.mTSFile = tsfile;
		this.mCAR = cad;
		reloadTrustManager();
	}

	public ReloadableX509TrustManager(final String tspath, final CertAcceptRequest cad) throws Exception {
		this(new File(tspath), cad);
	}

	@Override
	public void checkClientTrusted(final X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		LOGGER.log(Level.INFO, "checkClientTrusted()");
		mTrustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		LOGGER.log(Level.INFO, "checkServerTrusted()");
		try {
			mTrustManager.checkServerTrusted(chain, authType);
		} catch (java.security.cert.CertificateException cx) {
			LOGGER.log(Level.FINEST, "CertificateException");
			addServerCertAndReload(chain[0], true);
			mTrustManager.checkServerTrusted(chain, authType);
		}
		LOGGER.log(Level.FINEST, "checkServerTrusted() DONE");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		LOGGER.log(Level.INFO, "getAcceptedIssuers()");
		X509Certificate[] issuers = mTrustManager.getAcceptedIssuers();
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
		LOGGER.log(Level.INFO, "reloadTrustManager()");

		// load keystore from specified cert store (or default)
		KeyStore ts = KeyStore.getInstance("BKS");
		InputStream in;
		// Check if file exists and is not empty
		try {
			in = new FileInputStream(mTSFile);
			if (in.available() == 0) {
				in.close();
				in = null;
			}
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.INFO, "Truststore file {0} not found.", mTSFile.getName());
			in = null;
		}

		// Initialize empty truststore if none available
		if (in == null) {
			OutputStream out = new FileOutputStream(mTSFile);
			ts.load(null, "candis".toCharArray());
			ts.store(out, "candis".toCharArray());
			out.close();
			in = new FileInputStream(mTSFile);
			LOGGER.log(Level.INFO, "Initialized empty truststore {0}", mTSFile.getName());
		}

		// load truststore
		try {
			ts.load(in, null);
		} finally {
			in.close();
		}

		// add all temporary certs to KeyStore (ts)
		for (Certificate cert : mTempCertList) {
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
				mTrustManager = (X509TrustManager) tms[i];
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
	private void addServerCertAndReload(final X509Certificate cert, final boolean permanent) {

		// Call accept dialog if available, otherwise auto-accept
		if (mCAR != null) {
			mCAR.userCheckAccept(cert, this);

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
			LOGGER.log(Level.INFO, "Certificate ACCEPTED");
		} else {
			LOGGER.log(Level.INFO, "Certificate REJECTED");
		}


		try {
			if (permanent) {
				// import the cert into file trust store
				KeyStore ts = KeyStore.getInstance("BKS");

				// load truststore from file
				FileInputStream cert_istream = new FileInputStream(mTSFile);
				ts.load(cert_istream, "candis".toCharArray());
				cert_istream.close();

				// add certificate
				ts.setCertificateEntry("candiscert", cert);

				for (Enumeration<String> en = ts.aliases(); en.hasMoreElements();) {
					LOGGER.log(Level.INFO, "Alias found: {0}", en.nextElement());
				}

				// write truststore to file
				FileOutputStream cert_ostream = new FileOutputStream(mTSFile);
				ts.store(cert_ostream, "candis".toCharArray());
				cert_ostream.close();

				LOGGER.log(Level.FINE, "Added certificate (permanently)");
			} else {
				mTempCertList.add(cert);
				LOGGER.log(Level.FINE, "Added certificate (temporary)");
			}
			reloadTrustManager();
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.toString());
		}
	}

	/**
	 * Sets the cert accept dialog.
	 *
	 * @param cad Class that implements the CertAcceptRequest interface
	 */
	public void setCertAcceptDialog(final CertAcceptRequest cad) {
		this.mCAR = cad;
	}

	@Override
	public void acceptHandler(final boolean accept) {
		synchronized (accepted) {
			accepted.set(accept);
			accepted.notify();
		}
	}

	/**
	 *
	 * @param mdAlg
	 * @param cert
	 * @return
	 * @throws Exception
	 */
	public static String getCertFingerPrint(final String mdAlg, final Certificate cert)
					throws Exception {
		byte[] encCertInfo = cert.getEncoded();
		MessageDigest md = MessageDigest.getInstance(mdAlg);
		byte[] digest = md.digest(encCertInfo);
		return Utilities.toHexString(digest);
	}
}
