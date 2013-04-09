package candis.comm;

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
        implements X509TrustManager {

  private static final String TAG = X509TrustManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  /// Path to truststore
  private final File mTSFile;
  private X509TrustManager mTrustManager;
  /// List for temporary certificates
  private final List<Certificate> mTempCertList = new LinkedList<Certificate>();
  private CertAcceptRequestHandler mCAR;
  /// Boolean for synchronization
//  private final AtomicBoolean accepted = new AtomicBoolean(false);

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

  public ReloadableX509TrustManager(final File tsfile) throws Exception {
    this(tsfile, null);
  }

  /**
   * Creates instance of ReloadableX509TrustManager with choosable
   * ClientAcceptDialog (CAD).
   *
   * @param tsfile Path to truststore file (Will be created if not existent)
   * @param cad Implementation of CertAcceptDialog to enable user interaction
   * @throws Exception
   */
  public ReloadableX509TrustManager(final File tsfile, final CertAcceptRequestHandler cad) throws Exception {
    this.mTSFile = tsfile;
    if (cad == null) {
      mCAR = new DefaultAcceptHandler();
    }
    else {
      mCAR = cad;
    }
    reloadTrustManager();
  }

  public ReloadableX509TrustManager(final String tspath, final CertAcceptRequestHandler cad) throws Exception {
    this(new File(tspath), cad);
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] chain,
                                 String authType) throws java.security.cert.CertificateException {

    mTrustManager.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] chain,
                                 String authType) throws java.security.cert.CertificateException {

    try {
      mTrustManager.checkServerTrusted(chain, authType);
    }
    catch (java.security.cert.CertificateException cx) {
      LOGGER.log(Level.FINEST, "CertificateException");
      addServerCertAndReload(chain[0], true);
      mTrustManager.checkServerTrusted(chain, authType);
    }
    LOGGER.log(Level.FINEST, "checkServerTrusted() DONE");
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {

    X509Certificate[] issuers = mTrustManager.getAcceptedIssuers();
    return issuers;
  }

  /**
   * Reloads the trust manager from file.
   *
   * If the trustmanager file is empty or uninitalized, it will be initialized.
   *
   * @throws ExceptionaddServerCeratAndReload
   */
  private void reloadTrustManager() throws Exception {

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
    }
    catch (FileNotFoundException ex) {
      LOGGER.log(Level.INFO, String.format(
              "Truststore file %s not found.", mTSFile.getName()));
      in = null;
    }

    // Initialize empty truststore if none available
    if (in == null) {
      OutputStream out = new FileOutputStream(mTSFile);
      ts.load(null, "candis".toCharArray());
      ts.store(out, "candis".toCharArray());
      out.close();
      in = new FileInputStream(mTSFile);
      LOGGER.log(Level.INFO, String.format(
              "Initialized empty truststore %s", mTSFile.getName()));
    }

    // load truststore (no password)
    try {
      ts.load(in, null);
    }
    finally {
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
   * @return true if adding server cert succeeded, false otherwise
   */
  private void addServerCertAndReload(final X509Certificate cert, final boolean permanent) throws java.security.cert.CertificateException {

    // Calls handler...
    boolean accepted = mCAR.userCheckAccept(cert);

    if (accepted) {
      LOGGER.log(Level.INFO, "Certificate ACCEPTED");
    }
    else {
      LOGGER.log(Level.INFO, "Certificate REJECTED");
      throw new java.security.cert.CertificateException("User rejected Certificated");
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
          LOGGER.log(Level.INFO, String.format("Alias found: %s", en.nextElement()));
        }

        // write truststore to file
        FileOutputStream cert_ostream = new FileOutputStream(mTSFile);
        ts.store(cert_ostream, "candis".toCharArray());
        cert_ostream.close();

        LOGGER.log(Level.FINE, "Added certificate (permanently)");
      }
      else {
        mTempCertList.add(cert);
        LOGGER.log(Level.FINE, "Added certificate (temporary)");
      }
      reloadTrustManager();
    }
    catch (Exception ex) {
      LOGGER.log(Level.SEVERE, ex.toString());
    }
  }

  /**
   * Sets the cert accept dialog.
   *
   * @param carhandler Class that implements the CertAcceptRequest interface
   */
  public void setCertAcceptDialog(final CertAcceptRequestHandler carhandler) {
    mCAR = carhandler;
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

  /**
   * Return a default (always accepting) CertAcceptRequestHandler.
   *
   * @return
   */
  public CertAcceptRequestHandler getDefaultAcceptHandler() {
    return new DefaultAcceptHandler();
  }

  /**
   * Auto-accepts certificate.
   */
  private class DefaultAcceptHandler implements CertAcceptRequestHandler {

    public boolean userCheckAccept(X509Certificate cert) {
      return true;
    }

    public boolean hasResult() {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }
}
