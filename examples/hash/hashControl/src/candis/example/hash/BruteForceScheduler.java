package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.example.hash.HashInitParameter.HashType;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class BruteForceScheduler extends Scheduler implements ResultReceiver {

	public String resultValue;
	private final int mMaxDepth;
	private final BruteForceString mString;
	private BigInteger mDone;
	private BigInteger mTotal;

	class BruteForceString {

		private final List<Integer> mValues = new LinkedList<Integer>();
		private char[] mRange;
		boolean first = true;

		public BruteForceString(int initSize, char[] range) {
			mRange = range;
			if (initSize > 1) {
				mValues.add(mRange.length - 1);
			}
			for (int i = 0; i < initSize - 2; i++) {
				mValues.add(0);
			}
		}

		@Override
		public String toString() {
			String str = "";
			for (Integer c : mValues) {
				str += mRange[c];
			}
			return str;
		}

		public String inc() {
			if(!first){
				inc(0);
			}
			else{
				first = false;
			}
			return toString();
		}

		private void inc(int pos) {
			if (mValues.size() <= pos) {
				mValues.add(0);
			}
			else {
				Integer i = mValues.get(pos);
				i++;
				if (i >= mRange.length) {
					i = 0;
					inc(pos + 1);
				}
				mValues.set(pos, i);
			}
		}
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
							+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public BruteForceScheduler(int startLen, char[] chars, int maxDepth, HashType type, String hash) {
		super();
		init();
		mString = new BruteForceString(startLen - 1, chars);
		mMaxDepth = maxDepth;
		mDone = BigInteger.valueOf(0);
		mTotal = BigInteger.valueOf(0);
		System.out.println("chars.length " + chars.length);
		for (int i = startLen; i < maxDepth - 1; i++) {
			mTotal = mTotal.add(BigInteger.valueOf(chars.length).pow(i));
		}
		HashInitParameter init = new HashInitParameter(type, hexStringToByteArray(hash), chars);
		setInitialParameter(init);
	}

	private void init() {
		addResultReceiver(this);
	}

	@Override
	protected void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO) {
		Iterator<Map.Entry<String, DroidData>> it = droidList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, DroidData> droid = it.next();
			DroidData data = droid.getValue();
			String id = droid.getKey();
			it.remove();
			DistributedJobParameter param = popParameters();
			mRunningDroidsList.put(id, param);
			jobDistIO.startJob(id, param);
		}
	}

	@Override
	public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
		mDone = mDone.add(BigInteger.ONE);
		HashJobResult hashResult = (HashJobResult) result;
		HashJobParameter hashParam = (HashJobParameter) param;
		if (hashResult.mFoundValue) {
			try {
				resultValue = new String(hashParam.base, "UTF-8") + new String(hashResult.mValue);
				//resultValue = new String(hashResult.mValue, "UTF8");
			}
			catch (UnsupportedEncodingException ex) {
				Logger.getLogger(BruteForceScheduler.class.getName()).log(Level.SEVERE, null, ex);
			}
			this.abort();
		}
	}

	@Override
	protected boolean hasParametersLeft() {
		return parametersLeft() > 0;

	}

	@Override
	protected int parametersLeft() {
		// Abort if result found
		if (resultValue != null) {
			return 0;
		}
		return mParams.size() + mTotal.subtract(mDone).intValue();
	}

	@Override
	protected DistributedJobParameter popParameters() {
		if (mParams.size() > 0) {
			return mParams.pop();
		}
		try {
			return new HashJobParameter(mString.inc().getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException ex) {
			Logger.getLogger(BruteForceScheduler.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}
