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

  public Message(final Instruction req, final Serializable... data) {
    this.req = req;
    this.data = data;
  }

  /**
   * Currently the same as new Message()
   * @param instr
   * @param data
   * @return 
   */
  public static Message create(final Instruction instr, final Serializable... data) {
    return new Message(instr, data);
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
