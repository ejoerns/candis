package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Connection that allows to send Messages.
 *
 * @author Enrico Joerns
 */
public class MessageConnection extends ObjectConnection {

  private static final String TAG = MessageConnection.class.getName();
  private int msgID = 3;
  private static final int MAX_MSG_ID = 10;
  private static final LinkedList mMessageBuffer = new LinkedList();

  public MessageConnection(Socket socket) throws IOException {
    super(socket);
  }

  public MessageConnection(InputStream in, OutputStream out) {
    super(in, out);
  }

  public void sendMessage(final Message msg) throws IOException {
//		LOGGER.fine(String.format("##SEND to %s:%s: %s",
//															mSocket.getInetAddress(),
//															mSocket.getPort(),
//															msg.getRequest()));
    // write single objects to allow catching errors on receiver side
    CandisLog.v(TAG, "## Sending Request: " + msg.getRequest());
    sendObject(msg.getRequest());
    for (int idx = 0; idx < msg.getRequest().len; idx++) {
      CandisLog.v(TAG, "## Sending Data: " + msg.getData(idx));
      sendObject(msg.getData(idx));
    }
  }

  public Message readMessage() throws IOException {
    Instruction inst;
    try {
      // calc message id (mod MAX_MSG_ID)
      msgID = (msgID + 1) % MAX_MSG_ID;
      inst = (Instruction) receiveObject();
      CandisLog.v(TAG, "## Received Request: " + inst.toString());
    }
    catch (ClassNotFoundException ex) {
      CandisLog.e(TAG, ex.toString());
      inst = Instruction.NO_MSG;
    }
    List<Serializable> data = new LinkedList<Serializable>();
    // receive all data packages
    for (int idx = 0; idx < inst.len; idx++) {
      try {
        Serializable ser = receiveObject();
        data.add(ser);
        if (ser != null) {
          CandisLog.v(TAG, "## Received Data Type: " + ser.getClass());
        }
      }
      // if class not loadable, simply but raw data in
      catch (ClassNotFoundException ex) {
        CandisLog.d(TAG, ex.toString());
        data.add(getLastRawData());
      }
    }
    // concat Message from received data
    Serializable dataArray[] = new Serializable[data.size()];
    dataArray = data.toArray(dataArray);
    return new Message(inst, dataArray);
  }
}
