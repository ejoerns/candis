package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class TestHashRunnable implements DistributedRunnable {

  private static final int LOOPS = 10000;
  private TestHashInitParameter mInitial;
  private MessageDigest mMessageDigest;
  private boolean shouldStopNow = false;

  public TestHashRunnable() {
    try {
      mMessageDigest = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException ex) {
      Logger.getLogger(TestHashRunnable.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void stopJob() {
    shouldStopNow = true;
  }

  @Override
  public DistributedJobResult execute(DistributedJobParameter parameter) {
    // Cast incomming Parameter
    TestHashJobParameter p = (TestHashJobParameter) parameter;
    try {
      byte[] initFeed = mInitial.string.getBytes("UTF-8");
      byte[] paramFeed = p.string.getBytes("UTF-8");
      for (int i = 0; i < LOOPS && !shouldStopNow; i++) {
        mMessageDigest.reset();
        mMessageDigest.update(initFeed);
        mMessageDigest.update(paramFeed);
        mMessageDigest.digest();
      }
    }
    catch (UnsupportedEncodingException ex) {
      Logger.getLogger(TestHashRunnable.class.getName()).log(Level.SEVERE, null, ex);
    }
    return new TestHashJobResult(false);
  }

  public void setInitialParameter(DistributedJobParameter parameter) {
    mInitial = (TestHashInitParameter) parameter;
  }
}