package candis.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * Connection that allows to send Objects.
 *
 * @author Enrico Joerns
 */
public class ObjectConnection extends Connection {

  private static final String TAG = ObjectConnection.class.getName();
  private final ClassLoaderWrapper mClassLoaderWrapper;
  // Holds the raw data of the laste received element, can be used for deserialization
  private byte[] mRawData;

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

    mRawData = recieveChunk();

    if (mRawData == null) {
      CandisLog.w(TAG, "recieved null");
      return null;
    }

    CandisLog.v(TAG, "recieved " + mRawData.length + " bytes ...");

    Object obj = new ClassloaderObjectInputStream(new ByteArrayInputStream(mRawData), mClassLoaderWrapper).readObject();

    return (Serializable) obj;
  }

  /**
   * Returns the raw data for the last received Object.
   * Can be used to do manual deserialization later on.
   *
   * @return raw serialized data if sent with an ObjectConnection.
   */
  public byte[] getLastRawData() {
    return mRawData;
  }
}
