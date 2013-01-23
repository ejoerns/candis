package candis.example.hash;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for MiniTask.
 */
public class HashJobResult implements DistributedJobResult {

	/// Some value
	public final boolean mFoundValue;
	public final String mValue;

	public HashJobResult(boolean foundValue, String value) {
		mFoundValue = foundValue;
		mValue = value;
	}
}