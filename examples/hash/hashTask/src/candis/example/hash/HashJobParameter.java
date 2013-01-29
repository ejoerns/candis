package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class HashJobParameter implements DistributedJobParameter {

	public final byte[] base;
	public HashJobParameter(final byte[] base) {
		this.base = base;
	}
}