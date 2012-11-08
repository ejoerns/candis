package candis.client.comm;

import java.security.cert.X509Certificate;

/**
 * Interface that has to be implemented by classes to provid a user interface
 * for certificate acceptance.
 *
 * @author Enrico Joerns
 */
public interface CertAcceptRequest {

	/**
	 * Called by ReloadableX509TrustManager for certificate acceptance request.
	 *
	 * @param cert The cert that should be accepted
	 * @param cahandler A Handler that will be invoked if a result is available
	 */
	void userCheckAccept(final X509Certificate cert, final CertAcceptHandler cahandler);

	/// Maybe for pull mechanism...
	public boolean hasResult();
}
