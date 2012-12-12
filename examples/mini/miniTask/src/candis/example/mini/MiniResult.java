package candis.example.mini;

import candis.distributed.DistributedResult;
import java.io.Serializable;

/**
 * Serializable result for MiniTask.
 */
public class MiniResult extends DistributedResult implements Serializable {

	/// Some value
	public final float foobar;

	/**
	 * Initializes the result data.
	 *
	 * @param foobar
	 */
	public MiniResult(final float foobar) {
		this.foobar = foobar;
	}
}