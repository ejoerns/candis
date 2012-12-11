package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 * Serializable initial parameter for "global" settings.
 */
public class MiniInitParameter extends DistributedParameter implements Serializable {

	/// Some example Value
	public final int offset;

	/**
	 * Initializes the Initial Parameters for MiniTask
	 *
	 * @param offset some integer value
	 */
	public MiniInitParameter(final int offset) {
		this.offset = offset;
	}
}
