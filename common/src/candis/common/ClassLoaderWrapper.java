package candis.common;

/**
 *
 * @author Enrico Joerns
 */
public class ClassLoaderWrapper {

  private ClassLoader mDCL;

  public ClassLoaderWrapper() {
  }

  public ClassLoaderWrapper(ClassLoader cl) {
    mDCL = cl;
  }

  public void set(ClassLoader cl) {
    mDCL = cl;
  }

  public ClassLoader get() {
    return mDCL;
  }
}