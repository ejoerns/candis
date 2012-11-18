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
	private static final Logger logger = Logger.getLogger(TAG);
	/// Path to truststore
	private final File tsfile;
	private X509TrustManager trustManager;
	/// List for temporary certificates
	private final List<Certificate> tempCertList = new LinkedList<Certificate>();
	private CertAcceptRequest cad;
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
		this.setCertAcceptDialog(null);
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
		this.tsfile = tsfile;
		this.cad = cad;
		reloadTrustManager();
	}

	public ReloadableX509TrustManager(final String tspath, final CertAcceptRequest cad) throws Exception {
		this(new File(tspath), cad);
	}

	@Override
	public void checkClientTrusted(final X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		logger.log(Level.INFO, "checkClientTrusted()");
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
		logger.log(Level.INFO, "checkServerTrusted()");
		try {
			trustManager.checkServerTrusted(chain, authType);
		} catch (java.security.cert.CertificateException cx) {
			logger.log(Level.FINEST, "CertificateException");
			addServerCertAndReload(chain[0], true);
			trustManager.checkServerTrusted(chain, authType);
		}
		logger.log(Level.FINEST, "checkServerTrusted() DONE");
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
			in = new FileInputStream(tsfile);
			if (in.available() == 0) {
				in.close();
				in = null;
			}
		} catch (FileNotFoundException ex) {
			logger.log(Level.INFO, "Truststore file {0} not found.", tsfile.getName());
			in = null;
		}

		// Initialize empty truststore if none available
		if (in == null) {
			OutputStream out = new FileOutputStream(tsfile);
			ts.load(null, "candis".toCharArray());
			ts.store(out, "candis".toCharArray());
			out.close();
			in = new FileInputStream(tsfile);
			logger.log(Level.INFO, "Initialized empty truststore {0}", tsfile.getName());
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
	private void addServerCertAndReload(final X509Certificate cert, final boolean permanent) {

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
				FileInputStream cert_istream = new FileInputStream(tsfile);
				ts.load(cert_istream, "candis".toCharArray());
				cert_istream.close();

				// add certificate
				ts.setCertificateEntry("candiscert", cert);

				for (Enumeration<String> en = ts.aliases(); en.hasMoreElements();) {
					logger.log(Level.INFO, "Alias found: {0}", en.nextElement());
				}

				// write truststore to file
				FileOutputStream cert_ostream = new FileOutputStream(tsfile);
				ts.store(cert_ostream, "candis".toCharArray());
				cert_ostream.close();

				logger.log(Level.FINE, "Added certificate (permanently)");
			} else {
				tempCertList.add(cert);
				logger.log(Level.FINE, "Added certificate (temporary)");
			}
			reloadTrustManager();
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.toString());
		}
	}

	/**
	 * Sets the cert accept dialog.
	 *
	 * @param cad Class that implements the CertAcceptRequest interface
	 */
	public void setCertAcceptDialog(final CertAcceptRequest cad) {
		this.cad = cad;
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
