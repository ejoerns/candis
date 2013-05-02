package candis.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * Connection that allows to send Objects.
 *
 * @author Enrico Joerns
 */
public class ObjectConnection extends Connection {

  private static final String TAG = ObjectConnection.class.getName();
  // Holds the raw data of the laste received element, can be used for deserialization
  private byte[] mRawData;

  public ObjectConnection(Socket socket) throws IOException {
    super(socket);
  }

  public ObjectConnection(InputStream in, OutputStream out) {
    super(in, out);
  }

  /*
   * Converts Serializable object to byte array.
   *
   * Sends byte array with sendChunk().
   *
   */
  public void sendObject(Serializable object) throws IOException {

    byte[] bytes;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(object);

      bytes = baos.toByteArray();
    }
    finally {
      if (oos != null) {
        oos.close();
      }
    }

    CandisLog.v(TAG, String.format("sending %d bytes...", bytes.length));

    sendChunk(bytes);
  }

  /*
   * Reads byte array and returns serializable object.
   */
  public Serializable receiveObject() throws IOException, ClassNotFoundException {

    mRawData = recieveChunk();

    if (mRawData == null) {
      CandisLog.w(TAG, "recieved null");
      return null;
    }

    CandisLog.v(TAG, String.format("recieved %d bytes...", mRawData.length));

    ObjectInputStream ois = null;
    Object obj;
    try {
      ois = new ObjectInputStream(new ByteArrayInputStream(mRawData));
      obj = ois.readObject();
    }
    finally {
      if (ois != null) {
        ois.close();
      }
    }

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
