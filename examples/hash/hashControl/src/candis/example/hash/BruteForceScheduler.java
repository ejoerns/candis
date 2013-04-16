package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.example.hash.HashInitParameter.HashType;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
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
  private final BruteForceStringGenerator mStringGenerator;
  private long mDone;
  private long mTotal;
  private final int mClientDepth;
  int prepart = 0;
  int prepart2 = 0;

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
              + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  public BruteForceScheduler(int startLen, char[] alphabet, int maxDepth, int clientDepth, HashType type, String hash) {
    super();
    init();
    mStringGenerator = new BruteForceStringGenerator(startLen - clientDepth, alphabet, this);
    mMaxDepth = maxDepth;
    mDone = 0;
    mTotal = 0;
    mClientDepth = clientDepth;

    int d = Math.min(clientDepth, maxDepth);
    for (int i = 0; i <= Math.max(0, maxDepth - mClientDepth); i++) {
      mTotal += Math.pow(alphabet.length, i);
    }

    if (startLen <= d) {
      prepart = d - startLen + 1;
      prepart2 = prepart + startLen - 1;
      mTotal += prepart - 1;
    }

    HashInitParameter init = new HashInitParameter(type, hexStringToByteArray(hash), alphabet);
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
    mDone++;
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
    return mParams.size() + (int) (mTotal - mDone); // TODO: return long?
  }

  @Override
  protected DistributedJobParameter popParameters() {
    if (mParams.size() > 0) {
      return mParams.pop();
    }

    try {
      if (prepart > 0) {
        prepart--;
        return new HashJobParameter(mStringGenerator.toString().getBytes("UTF-8"), prepart2 - prepart);
      }
      return new HashJobParameter(mStringGenerator.nextString().getBytes("UTF-8"), Math.min(mClientDepth, mMaxDepth));
    }
    catch (UnsupportedEncodingException ex) {
      Logger.getLogger(BruteForceScheduler.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
