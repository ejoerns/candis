package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class HashJobParameter implements DistributedJobParameter {

	public final byte[] base;
	public final int depth;

	public HashJobParameter(final byte[] base, int depth) {
		this.base = base;
		this.depth = depth;
	}
}