package candis.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.net.Socket;

/**
 * Connection that allows to send Objects.
 *
 * @author Enrico Joerns
 */
public class ObjectConnection extends Connection {

  private static final boolean DEBUG = true;
  private static final String TAG = ObjectConnection.class.getName();
  private final ClassLoaderWrapper mClassLoaderWrapper;

  public ObjectConnection(Socket socket, ClassLoaderWrapper clw) {
    super(socket);
    mClassLoaderWrapper = clw;
  }

  /*
   * Wandelt ein Serializable Objekt in ein Byte-Array um.
   *
   * Verschickt dieses Byte-Array mit sendChunk().
   *
   */
  public void sendObject(Serializable object) {

    try {

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
      oos.close();

      byte[] bytes = baos.toByteArray();

      CandisLog.v(TAG, "sending " + bytes.length + " bytes ...");

      sendChunk(bytes);                                              // datenarray wird weitergegeben

    }
    catch (IOException ex) {

      CandisLog.e(TAG, "Sending object failed!");
      CandisLog.e(TAG, ex.getMessage());
      try {
        closeSocket();
      }
      catch (IOException ioex) {
        CandisLog.e(TAG, ioex.getMessage());
      }
    }
  }

  /*
   * Liest Byte Array mit recieveChunk aus dem Stream.
   *
   * Gibt serialisierbares Objekt zurück.
   *
   */
  public Serializable receiveObject() throws IOException, ClassNotFoundException {

    byte[] a = recieveChunk();

    if (a == null) {
      CandisLog.w(TAG, "recieved null");
      return null;
    }

    CandisLog.v(TAG, "recieved " + a.length + " bytes ...");

    Object obj = new ClassloaderObjectInputStream(new ByteArrayInputStream(a)).readObject();

    return (Serializable) obj;
  }

  /**
   * Resolves class with custom classloader.
   */
  public class ClassloaderObjectInputStream extends ObjectInputStream {

    @Override
    public Class resolveClass(ObjectStreamClass desc) throws IOException,
            ClassNotFoundException {

      try {
        return mClassLoaderWrapper.get().loadClass(desc.getName());
      }
      catch (Exception e) {
      }

      return super.resolveClass(desc);
    }

    public ClassloaderObjectInputStream(InputStream in) throws IOException {
      super(in);
    }
  }
}
