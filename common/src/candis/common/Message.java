package candis.common;

import java.io.Serializable;

/**
 * Data packet that is transmitted between client and server.
 *
 * @author enrico
 */
public class Message implements Serializable {

  private Serializable[] data;
  private Instruction req;
  private static final Message mInstance = new Message(Instruction.NO_MSG);

  private Message(final Instruction req, final Serializable... data) {
    this.req = req;
    this.data = data;
  }

  public static Message create(final Instruction instr) {
    mInstance.req = instr;
    mInstance.data = null;
    return mInstance;
  }

  public static Message create(final Instruction instr, final Serializable... data) {
    mInstance.req = instr;
    mInstance.data = data;
    return mInstance;
  }

  public Instruction getRequest() {
    return req;
  }

  public Serializable getData(final int idx) {
    if (data == null) {
      return null;
    }
    return data[idx];
  }

  public Serializable[] getData() {
    return data;
  }
}
