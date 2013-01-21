package candis.common;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Enrico Joerns
 */
public final class ByteArray implements Serializable {

	private final byte[] data;

	public ByteArray(byte[] data) {
		if (data == null) {
			throw new NullPointerException();
		}
		this.data = data;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ByteArray)) {
			return false;
		}
		return Arrays.equals(data, ((ByteArray) other).data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}
}
