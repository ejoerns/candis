package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
		for (int i = 0; i < mInitial.range.length; i++) {
			if (shouldStopNow) {
				return new HashJobResult(false, null);
			}
			byte[] result = doHash(p.base, i);
			if (Arrays.equals(mInitial.hash, result)) {
				return new HashJobResult(true, new char[]{mInitial.range[i]});

			}
		}
		return new HashJobResult(false, null);
	}

	public byte[] doHash(byte[] base, int index) {
		try {
			mMessageDigest.reset();
			mMessageDigest.update(base);
			mMessageDigest.update(Character.toString(mInitial.range[index]).getBytes("UTF-8"));
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