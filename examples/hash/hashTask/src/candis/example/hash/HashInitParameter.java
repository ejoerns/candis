package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable initial parameter for "global" settings.
 */
public class HashInitParameter implements DistributedJobParameter {

	/// Some example Value
	public final int offset;

	/**
	 * Initializes the Initial Parameters for MiniTask
	 *
	 * @param offset some integer value
	 */
	public HashInitParameter(final int offset) {
		this.offset = offset;
	}
}
