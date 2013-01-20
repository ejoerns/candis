package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Designed to run in separate thread for every connected client.
 *
 * @author enrico
 */
public class Connection {

  private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());
  private final Socket mSocket;

  public Connection(final Socket socket) {
    mSocket = socket;
//    try {
//      mSocket.setSoTimeout(5000);                      // Unterbricht blockierendes read() beim Client wenn Server nichts sendet.
//    }
//    catch (SocketException ex) {
//      Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
//    }
  }

  protected void initConnection() {
    LOGGER.log(Level.INFO, "Client {0} connected...", mSocket.getInetAddress());
  }

  /**
   * Überträgt Datenpaket beliebiger Größe.
   *
   * @param bytes Byte-Array
   */
  public void sendChunk(byte[] bytes) throws IOException {

    // send header (length[4]) and body of message
    mSocket.getOutputStream().write(IntToByteArray(bytes.length));                                             // header wird als erstes versendet
    mSocket.getOutputStream().write(bytes);

  }

  /**
   * Empfängt Datenpakete (blocking).
   *
   * @return Byte-Array
   */
  public byte[] recieveChunk() throws IOException {

    // Stream -> bytes []
    InputStream in;
    byte[] header = new byte[4];
    byte[] bytes = null;
    int offset = 0;

    in = mSocket.getInputStream();

    if (in == null) {
      return null;
    }

    // read header
    for (int i = 0; i < 4; i++) {
      header[i] = (byte) mSocket.getInputStream().read();                   // 4 byte header wird zuerst gelesen
      // If read failed, try again.
      if (header[i] == -1) {
        i--;
      }
    }
    int dataLength = byteArrayToInt(header);       // Puffergröße wird bestimmt

    if (dataLength > 0) {
      bytes = new byte[dataLength];                  // Datenpuffer   
    }
    else {
      System.out.println("EE: Array size zero or negative!");
      return null;
    }

    // read bytes from input stream to byte array
    do {
      offset += in.read(bytes, offset, dataLength - offset);                   // 
    }
    while (offset < dataLength);

    return bytes;
  }

  /**
   * Konvertiert 4-Byte-Array in Integer.
   *
   * @param bytes 4-Byte-Array
   * @return Integer
   */
  private int byteArrayToInt(byte[] bytes) {

    int value = (0xFF & bytes[0]) << 24;
    value += (0xFF & bytes[1]) << 16;
    value += (0xFF & bytes[2]) << 8;
    value += 0xFF & bytes[3];

    return value;
  }

  /**
   * Konvertiert Integer in 4-Byte-Array.
   *
   * @param value Integer-Wert
   * @return 4-Byte-Array
   */
  private byte[] IntToByteArray(int value) {

    byte[] bytes = new byte[4];
    bytes[0] = (byte) (value >>> 24);
    bytes[1] = (byte) (value >>> 16);
    bytes[2] = (byte) (value >>> 8);
    bytes[3] = (byte) (value);

    return bytes;
  }

  protected boolean isSocketClosed() {
    return mSocket.isClosed();
  }

  protected void closeSocket() throws IOException {
    if (!mSocket.isClosed()) {
      try {
        mSocket.close();
      }
      catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * ObjectInputStream that allows to specify custom ClassLoader.
   */
  private class ClassLoaderObjectInputStream extends ObjectInputStream {

    private final ClassLoaderWrapper mClassLoaderWrapper;

    @Override
    public Class resolveClass(ObjectStreamClass desc) throws IOException,
            ClassNotFoundException {

      try {
        return mClassLoaderWrapper.get().loadClass(desc.getName());
      }
      catch (Exception e) {
        return super.resolveClass(desc);
      }
    }

    public ClassLoaderObjectInputStream(InputStream in, ClassLoaderWrapper cloaderwrap) throws IOException {
      super(in);
      mClassLoaderWrapper = cloaderwrap;
    }
  }
}
