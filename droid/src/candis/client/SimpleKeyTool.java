/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author enrico
 */
public class SimpleKeyTool {

	private KeyStore keyStore;
	private static final java.util.ResourceBundle rb =
					java.util.ResourceBundle.getBundle("sun.security.util.Resources");
	private CertificateFactory cf = null;
	private boolean noprompt = false;
	private KeyStore caks = null; // "cacerts" keystore

	public SimpleKeyTool() {
		try {
			keyStore = KeyStore.getInstance("BKS");
		} catch (KeyStoreException ex) {
			Logger.getLogger(SimpleKeyTool.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Imports a certificate and adds it to the list of trusted certificates.
	 *
	 * @return true if the certificate was added, otherwise false.
	 */
	/*
	 private boolean addTrustedCert(String alias, InputStream in)
	 throws Exception {
	 if (alias == null) {
	 throw new Exception(rb.getString("Must.specify.alias"));
	 }
	 if (keyStore.containsAlias(alias)) {
	 MessageFormat form = new MessageFormat(rb.getString("Certificate.not.imported.alias.alias.already.exists"));
	 Object[] source = {alias};
	 throw new Exception(form.format(source));
	 }

	 // Read the certificate
	 X509Certificate cert = null;
	 try {
	 cert = (X509Certificate) cf.generateCertificate(in);
	 } catch (ClassCastException cce) {
	 throw new Exception(rb.getString("Input.not.an.X.509.certificate"));
	 } catch (CertificateException ce) {
	 throw new Exception(rb.getString("Input.not.an.X.509.certificate"));
	 }

	 // if certificate is self-signed, make sure it verifies
	 boolean selfSigned = false;
	 if (isSelfSigned(cert)) {
	 cert.verify(cert.getPublicKey());
	 selfSigned = true;
	 }

	 if (noprompt) {
	 keyStore.setCertificateEntry(alias, cert);
	 return true;
	 }

	 // check if cert already exists in keystore
	 String reply = null;
	 String trustalias = keyStore.getCertificateAlias(cert);
	 if (trustalias != null) {
	 MessageFormat form = new MessageFormat(rb.getString("Certificate.already.exists.in.keystore.under.alias.trustalias."));
	 Object[] source = {trustalias};
	 System.err.println(form.format(source));
	 //			reply = getYesNoReply(rb.getString("Do.you.still.want.to.add.it.no."));
	 reply = "YES";
	 } else if (selfSigned) {
	 if (trustcacerts && (caks != null)
	 && ((trustalias = caks.getCertificateAlias(cert)) != null)) {
	 MessageFormat form = new MessageFormat(rb.getString("Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias."));
	 Object[] source = {trustalias};
	 System.err.println(form.format(source));
	 reply = getYesNoReply(rb.getString("Do.you.still.want.to.add.it.to.your.own.keystore.no."));
	 }
	 if (trustalias == null) {
	 // Print the cert and ask user if they really want to add
	 // it to their keystore
	 //				printX509Cert(cert, System.out);
	 reply = getYesNoReply(rb.getString("Trust.this.certificate.no."));
	 }
	 }
	 if (reply != null) {
	 if ("YES".equals(reply)) {
	 keyStore.setCertificateEntry(alias, cert);
	 return true;
	 } else {
	 return false;
	 }
	 }

	 // Try to establish trust chain
	 try {
	 Certificate[] chain = establishCertChain(null, cert);
	 if (chain != null) {
	 keyStore.setCertificateEntry(alias, cert);
	 return true;
	 }
	 } catch (Exception e) {
	 // Print the cert and ask user if they really want to add it to
	 // their keystore
	 //			printX509Cert(cert, System.out);
	 //			reply = getYesNoReply(rb.getString("Trust.this.certificate.no."));
	 reply = "YES";
	 if ("YES".equals(reply)) {
	 keyStore.setCertificateEntry(alias, cert);
	 return true;
	 } else {
	 return false;
	 }
	 }

	 return false;
	 }
	 */
	/**
	 * Returns true if the certificate is self-signed, false otherwise.
	 */
	private boolean isSelfSigned(X509Certificate cert) {
		return signedBy(cert, cert);
	}

	private boolean signedBy(X509Certificate end, X509Certificate ca) {
		if (!ca.getSubjectDN().equals(end.getIssuerDN())) {
			return false;
		}
		try {
			end.verify(ca.getPublicKey());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
