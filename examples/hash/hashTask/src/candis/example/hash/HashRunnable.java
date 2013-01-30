package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class HashRunnable implements DistributedRunnable {

	private HashInitParameter mInitial;
	private MessageDigest mMessageDigest;
	private boolean shouldStopNow = false;

	@Override
	public void stopJob() {
		shouldStopNow = true;
	}

	@Override
	public DistributedJobResult runJob(DistributedJobParameter parameter) {
		// Cast incomming Parameter
		HashJobParameter p = (HashJobParameter) parameter;
		int[] block = new int[p.depth];
		//System.out.println(new String(p.base));
		while (inc(block, p.depth - 1)) {

			if (shouldStopNow) {
				return new HashJobResult(false, null);
			}
			byte[] result = doHash(p.base, block);
			if (Arrays.equals(mInitial.hash, result)) {
				char[] found = new char[p.depth];
				for (int i = 0; i < p.depth; i++) {
					found[i] = mInitial.range[block[i]];
				}
				return new HashJobResult(true, found);

			}

		}
		return new HashJobResult(false, null);
	}

	private boolean inc(int[] array, int pos) {
		if (pos < 0) {
			return false;
		}
		array[pos]++;
		if (array[pos] >= mInitial.range.length) {
			array[pos] = 0;
			return inc(array, pos - 1);
		}
		return true;
	}

	public byte[] doHash(byte[] base, int[] block) {
		try {
			mMessageDigest.reset();
			mMessageDigest.update(base);
			for (int i : block) {
				mMessageDigest.update(Character.toString(mInitial.range[i]).getBytes("UTF-8"));
			}
			return mMessageDigest.digest();
		}
		catch (UnsupportedEncodingException ex) {
			Logger.getLogger(HashRunnable.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public void setInitialParameter(DistributedJobParameter parameter) {
		mInitial = (HashInitParameter) parameter;
		try {
			mMessageDigest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(HashRunnable.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}