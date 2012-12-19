package candis.example.mini;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for MiniTask.
 */
public class MiniJobResult implements DistributedJobResult {

	/// Some value
	public final float foobar;

	/**
	 * Initializes the result data.
	 *
	 * @param foobar
	 */
	public MiniJobResult(final float foobar) {
		this.foobar = foobar;
	}
}