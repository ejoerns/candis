package candis.client.comm;

/**
 * Handler interface for certificate accept dialog.
 *
 * Has to be implemented by classes that should provide a handling for
 * results from a CertAcceptRequest
 *
 * @author Enrico Joerns
 */
public interface CertAcceptHandler {

	/**
	 * Called by certificate dialog.
	 *
	 * @param accept Return value of certificate dialog. 'true' if user accepted
	 * certificate, 'false' if user rejected certificate
	 */
	public void acceptHandler(boolean accept);
}
