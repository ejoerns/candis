/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client.comm;

import java.security.cert.X509Certificate;

/**
 *
 * @author enrico
 */
public interface CertAcceptRequest {

	/**
	 *
	 * @return
	 */
	void userCheckAccept(X509Certificate cert, final CertAcceptHandler cahandler);

	boolean hasResult();

	public interface Listener {

		public boolean getCheckResult();
	}
}
