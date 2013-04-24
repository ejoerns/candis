package candis.example.hash;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.parameter.BooleanUserParameter;
import candis.distributed.parameter.IntegerUserParameter;
import candis.distributed.parameter.StringListUserParameter;
import candis.distributed.parameter.StringUserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import candis.example.hash.HashInitParameter.HashType;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashControl extends DistributedControl implements ResultReceiver {

  public String mResultValue;
  private int mMaxDepth;
  private BruteForceStringGenerator mStringGenerator;
  private long mDone;
  private long mTotal;
  private int mClientDepth;
  int prepart = 0;
  int prepart2 = 0;
  private int mParamsDone = 0;
  private HashInitParameter init;

  @Override
  public void init() {
    UserParameterSet parameters = new UserParameterSet();
    // 900150983cd24fb0d6963f7d28e17f72 = md5("abc");
    // e99a18c428cb38d5f260853678922e03 = md5("abc123")
    StringUserParameter hashvalue = new StringUserParameter("hash.hashvalue", "Hash (hex)", "Enter hash to crack here",
                                                            "e99a18c428cb38d5f260853678922e03", new HashInputValidator());
    parameters.AddParameter(hashvalue);

    StringListUserParameter type = new StringListUserParameter("hash.type", "Hash-Method", "Specifiy the type of the hash",
                                                               0, new String[]{"md5", "sha1"});
    parameters.AddParameter(type);

    StringListUserParameter tryAlpha = new StringListUserParameter("hash.try.alpha", "Characters", "Use small charactes (a-z), caps (A-Z) or both.",
                                                                   2, new String[]{"small", "caps", "both", "none"});
    parameters.AddParameter(tryAlpha);

    BooleanUserParameter tryNumeric = new BooleanUserParameter("hash.try.numeric", "Numbers", "Use Numbers",
                                                               false);
    parameters.AddParameter(tryNumeric);

    StringUserParameter tryElse = new StringUserParameter("hash.try.else", "Other Chars", "Enter other Characters to try",
                                                          "", null);
    parameters.AddParameter(tryElse);

    IntegerUserParameter start = new IntegerUserParameter("hash.trylen.start", "Minimal Length", "Specify the minimal length of the brutefoce string",
                                                          3, 1, Integer.MAX_VALUE, 1, new SmallerThanValidator("hash.trylen.stop"));
    parameters.AddParameter(start);

    IntegerUserParameter stop = new IntegerUserParameter("hash.trylen.stop", "Maximal Length", "Specify the maximal length of the bruteforce string",
                                                         6, 1, Integer.MAX_VALUE, 1, new BiggerThanValidator("hash.trylen.start"));
    parameters.AddParameter(stop);

    IntegerUserParameter depth = new IntegerUserParameter("hash.depth", 2, 1, 10, 1, null);
    parameters.AddParameter(depth);

    UserParameterRequester.getInstance().request(parameters);

    System.out.println("hashvalue " + hashvalue.getValue());
    System.out.println("type " + type.getValue());
    System.out.println("try.alpha " + tryAlpha.getValue());
    System.out.println("try.numeric " + tryNumeric.getBooleanValue());
    System.out.println("try.else " + tryElse.getValue());
    System.out.println("trylen.start " + start.getIntegerValue());
    System.out.println("trylen.stop " + stop.getIntegerValue());
    System.out.println("depth " + depth.getIntegerValue());
    //parameters

    String alpha = tryAlpha.getValue().toString();
    String alphabet = "";
    if (tryNumeric.getBooleanValue()) {
      alphabet += "0123456789";
    }
    if (alpha.equals("both") || alpha.equals("small")) {
      alphabet += "abcdefghijklmnopqrstuvwxyz";
    }
    if (alpha.equals("both") || alpha.equals("caps")) {
      alphabet += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }

    for (char c : tryElse.getStringValue().toCharArray()) {
      // Prevent duplicates
      if (!alphabet.contains(Character.toString(c))) {
        alphabet += c;
      }
    }

    // Generate BruteForceScheduler
    HashType typeValue = HashType.MD5;
    if (type.getValue().toString().equals("sha1")) {
      typeValue = HashType.SHA1;
    }

    mStringGenerator = new BruteForceStringGenerator(start.getIntegerValue() - depth.getIntegerValue(), alphabet.toCharArray());
    mMaxDepth = stop.getIntegerValue();
    mDone = 0;
    mTotal = 0;
    prepart = prepart2 = 0;
    mClientDepth = depth.getIntegerValue();

    int d = Math.min(depth.getIntegerValue(), stop.getIntegerValue());
    for (int i = 0; i <= Math.max(0, stop.getIntegerValue() - mClientDepth); i++) {
      mTotal += Math.pow(alphabet.toCharArray().length, i);
    }

    if (start.getIntegerValue() <= d) {
      prepart = d - start.getIntegerValue() + 1;
      prepart2 = prepart + start.getIntegerValue() - 1;
      mTotal += prepart - 1;
    }

    init = new HashInitParameter(typeValue, hexStringToByteArray(hashvalue.getStringValue()), alphabet.toCharArray());

    System.out.println("" + mTotal + " parameters added");

  }

  @Override
  public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
    mDone++;
    HashJobResult hashResult = (HashJobResult) result;
    HashJobParameter hashParam = (HashJobParameter) param;
    if (hashResult.mFoundValue) {
      try {
        mResultValue = new String(hashParam.base, "UTF-8") + new String(hashResult.mValue);
        //resultValue = new String(hashResult.mValue, "UTF8");
      }
      catch (UnsupportedEncodingException ex) {
        Logger.getLogger(HashControl.class.getName()).log(Level.SEVERE, null, ex);
      }
//      mScheduler.abort();
    }
  }

  @Override
  public final void onSchedulerDone() {
    if (mResultValue == null) {
      System.out.println("nothing");
    }
    else {
      System.out.println(mResultValue);
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

  @Override
  public final DistributedJobParameter getParameter() {
    try {
      if (prepart > 0) {
        prepart--;
        mParamsDone++;
        return new HashJobParameter(mStringGenerator.toString().getBytes("UTF-8"), prepart2 - prepart);
      }
      mParamsDone++;
      return new HashJobParameter(mStringGenerator.nextString().getBytes("UTF-8"), Math.min(mClientDepth, mMaxDepth));
    }
    catch (UnsupportedEncodingException ex) {
      Logger.getLogger(HashControl.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  @Override
  public final long getParametersLeft() {
    return mTotal - mParamsDone;
  }

  @Override
  public final boolean hasParametersLeft() {
    return ((mTotal - mParamsDone) > 0) ? true : false;
  }

  @Override
  public DistributedJobParameter getInitialParameter() {
    return init;
  }
}
