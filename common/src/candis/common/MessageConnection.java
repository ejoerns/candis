package candis.common;

import java.io.IOException;
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

  public MessageConnection(Socket socket, ClassLoaderWrapper clw) {
    super(socket, clw);
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
    Instruction inst = null;
    try {
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
      catch (ClassNotFoundException ex) {
        CandisLog.e(TAG, ex.toString());
        data.add(null);
      }
    }
    // concat Message from received data
    Serializable dataArray[] = new Serializable[data.size()];
    dataArray = data.toArray(dataArray);
    return new Message(inst, dataArray);
  }
}
