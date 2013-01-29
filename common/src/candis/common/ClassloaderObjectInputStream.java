package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * ObjectInputStream that allows to specify custom ClassLoader.
 */
public class ClassloaderObjectInputStream extends ObjectInputStream {

  private final ClassLoader mClassLoader;

  public ClassloaderObjectInputStream(InputStream in, ClassLoader cloader) throws IOException {
    super(in);
    mClassLoader = cloader;
  }

  @Override
  public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    try {
      return mClassLoader.loadClass(desc.getName());
    }
    catch (Exception e) {
      return super.resolveClass(desc);
    }
  }
}
