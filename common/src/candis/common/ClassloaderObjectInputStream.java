/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * ObjectInputStream that allows to specify custom ClassLoader.
 */
public class ClassloaderObjectInputStream extends ObjectInputStream {

  private final ClassLoaderWrapper mClassLoaderWrapper;

  public ClassloaderObjectInputStream(InputStream in, ClassLoaderWrapper cloaderwrap) throws IOException {
    super(in);
    mClassLoaderWrapper = cloaderwrap;
  }

  @Override
  public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    try {
      return mClassLoaderWrapper.get().loadClass(desc.getName());
    }
    catch (Exception e) {
      return super.resolveClass(desc);
    }
  }
}
