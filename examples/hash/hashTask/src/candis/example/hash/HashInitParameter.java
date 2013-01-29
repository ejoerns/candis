package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable initial parameter for "global" settings.
 */
public class HashInitParameter implements DistributedJobParameter {

	public enum HashType {

		MD5,
		SHA1;
	};
	public final HashType type;
	public final byte[] hash;
	public final char[] range;

	public HashInitParameter(HashType type, byte[] hash, char[] range) {
		this.type = type;
		this.hash = hash;
		this.range = range;

	}
}
