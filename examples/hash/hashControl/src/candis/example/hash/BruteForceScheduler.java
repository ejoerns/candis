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
	private final int mClientDepth;
	int prepart = 0;
	int prepart2 = 0;

	class BruteForceString {

		private final List<Integer> mValues = new LinkedList<Integer>();
		private char[] mRange;
		//boolean first = true;

		public BruteForceString(int initSize, char[] range) {
			if (initSize < 0) {
				initSize = 0;
			}
			mRange = range;
			if (initSize > 0) {
				//first = false;

				if (initSize > 1) {
					mValues.add(mRange.length - 1);
				}
			}
			for (int i = 0; i < initSize - 2; i++) {
				mValues.add(0);

			}
		}

		@Override
		public String toString() {
			String str = "";
			for (Integer c : mValues) {
				str = mRange[c] + str;
			}
			return str;
		}

		public String inc() {
			//if (!first) {
			inc(0);
			/*}
			 else {
			 first = false;
			 }*/
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

	public BruteForceScheduler(int startLen, char[] chars, int maxDepth, int clientDepth, HashType type, String hash) {
		super();
		init();
		mString = new BruteForceString(startLen - clientDepth, chars);
		mMaxDepth = maxDepth;
		mDone = BigInteger.valueOf(0);
		mTotal = BigInteger.valueOf(0);
		mClientDepth = clientDepth;

		int d = Math.min(clientDepth, maxDepth);
		for (int i = 0; i <= Math.max(0, maxDepth - mClientDepth); i++) {
			mTotal = mTotal.add(BigInteger.valueOf(chars.length).pow(i));
		}

		if (startLen <= d) {
			prepart = d - startLen + 1;
			prepart2 = prepart + startLen - 1;
			mTotal = mTotal.add(BigInteger.valueOf(prepart - 1));
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
		return getParametersLeft() > 0;

	}

	@Override
	public int getParametersLeft() {
		// Abort if result found
		if (resultValue != null) {
			return 0;
		}
		return mParams.size() + mTotal.subtract(mDone).intValue();
	}
	/*
	 private static void sd(int start, int stop, int step) {
	 System.out.printf("%d -> %d (%d)\n", start, stop, step);
	 BruteForceScheduler bs = new BruteForceScheduler(start, new char[]{'0', '1',}, stop, step, HashType.MD5, "ab");
	 while (bs.hasParametersLeft()) {
	 HashJobParameter p = (HashJobParameter) bs.popParameters();
	 bs.mDone = bs.mDone.add(BigInteger.ONE);
	 System.out.print(new String(p.base));
	 for (int i = 0; i < p.depth; i++) {
	 System.out.print("x");
	 }
	 System.out.println(" " + p.depth);
	 }
	 System.out.println("");
	 }

	 public static void main(String[] argv) {

	 sd(1, 1, 1);
	 sd(1, 1, 2);
	 sd(1, 2, 1);
	 sd(2, 2, 1);
	 sd(1, 3, 4);
	 sd(1, 5, 4);
	 sd(3, 6, 4);
	 }*/

	@Override
	protected DistributedJobParameter popParameters() {
		if (mParams.size() > 0) {
			return mParams.pop();
		}


		try {
			if (prepart > 0) {
				prepart--;
				return new HashJobParameter(mString.toString().getBytes("UTF-8"), prepart2 - prepart);
			}
			return new HashJobParameter(mString.inc().getBytes("UTF-8"), Math.min(mClientDepth, mMaxDepth));
		}
		catch (UnsupportedEncodingException ex) {
			Logger.getLogger(BruteForceScheduler.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}
