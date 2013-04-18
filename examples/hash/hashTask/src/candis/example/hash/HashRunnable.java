package candis.example.hash;

import android.util.Log;
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
      String test = "";
      for (int i : block) {
        test += (Character.toString(mInitial.range[i]));
      }
      byte[] result = doHash(p.base, block);

      System.out.println(new String(p.base) + test + ", " + bytesToHex(mInitial.hash) + ", " + bytesToHex(result));

      if (Arrays.equals(mInitial.hash, result)) {
        char[] found = new char[p.depth];
        for (int i = 0; i < p.depth; i++) {
          found[i] = mInitial.range[block[i]];
        }
        Log.e(HashRunnable.class.getName(), "found result!");
        return new HashJobResult(true, found);

      }

    }
    return new HashJobResult(false, null);
  }

  public static String bytesToHex(byte[] bytes) {
    final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    char[] hexChars = new char[bytes.length * 2];
    int v;
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
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

  /**
   *
   * @param base
   * @param block
   * @return
   */
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
    Log.e(HashRunnable.class.getName(), "Initial parameter: Hash set to " + new String(mInitial.hash));
    Log.e(HashRunnable.class.getName(), "                  Range set to " + new String(mInitial.range));
    try {
      mMessageDigest = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException ex) {
      Logger.getLogger(HashRunnable.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}