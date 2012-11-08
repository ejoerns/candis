/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client.comm;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;

/**
 *
 * @author Enrico Joerns
 */
public class CertificateCreator {

	CertificateFactory cf;
	X509Certificate newCert;
//	X500Name name;

	public void foo() {
		try {
			cf = CertificateFactory.getInstance("X509");
		} catch (CertificateException ex) {
			Logger.getLogger(CertificateCreator.class.getName()).log(Level.SEVERE, null, ex);
		}
		
//		name = new X500Name(commonName, organizationalUnit, organization,
//                                city, state, country);
		X500Principal xp = new X500Principal("O=SomeOrg, OU=SomeOrgUnit, C=US");
//		CertAndKeyGen keypair =
//                new CertAndKeyGen(keyAlgName, sigAlgName, providerName);
//		newCert = (X509Certificate)cf.generateCertificate(bais);
	}
}
