package candis.common;

import candis.common.fsm.Transition;

/**
 * Message enum type.
 *
 * Serialize: CandisMsg mymessage = CandisMsg.GET_INFO; int simpleVal =
 * mymessage.toValue();
 *
 * Deserialize: CandisMsg newmessage = CandisMsg.fromValue(simpleVal);
 *
 * @author enrico
 */
public enum Instruction implements Transition {

  // -- Available messages
  /// Indicates no message available
  NO_MSG(0, 0),
  /// Remote error occured
  ERROR(1, 0),
  /// Ack
  ACK(2, 0),
  /// Nack
  NACK(3, 0),
  /// ping
  PING(4, 0),
  /// pong
  PONG(5, 0),
  /// Request information - unused
  GET_INFO(10, 0),
  /// Send information - unused
  SEND_INFO(20, 1, Type.STRING),
  /// Droid requests connection to master
  REQUEST_CONNECTION(40, 1, Type.OBJECT),
  /// Master requests check code
  REQUEST_CHECKCODE(50, 0),
  /// Droid sends check code (checkcode)
  SEND_CHECKCODE(55, 1, Type.STRING),
  /// Master says that checkcode is invalid
  //  INVALID_CHECKCODE(56, 0),
  /// Master requests profile data from droid
  //  REQUEST_PROFILE(60, 0),
  /// Droid registeres at master (ID, Profile)
  REGISTER(61, 2, Type.OBJECT, Type.OBJECT),
  /// Droid sends profile data
  //  SEND_PROFILE(65, 1, Type.STRING),
  /// Master accepts connection
  //  ACCEPT_CONNECTION(70, 0),
  /// Master rejects connection
  //  REJECT_CONNECTION(80, 0),
  /// Master sends Job binary (taskID, binary, iparameter)
  SEND_BINARY(95, 3, Type.STRING, Type.OBJECT, Type.OBJECT),
  /// Master sends Inital Parameter (taskID, iparameter)
  SEND_INITIAL(96, 2, Type.STRING, Type.OBJECT),
  /// Master sends job (taskID, jobID, parameter)
  SEND_JOB(100, 3, Type.STRING, Type.STRING, Type.OBJECT),
  /// Droid sends result to master (taskID, jobID, result, calctime)
  SEND_RESULT(105, 4, Type.STRING, Type.STRING, Type.OBJECT, Type.LONG),
  /// Droid/Master informs that it will terminate
  SELF_TERMINATE(110, 0),
  /// Droid wants to disconnect from master
  DISCONNECT(120, 0),
  /// Droid requests binary
  REQUEST_BINARY(130, 1, Type.STRING),
  /// Droid requests initial parameter
  REQUEST_INITIAL(140, 1, Type.STRING);
  // --
  private final int val;
  public final int len;
  private final Type[] types;

  Instruction(int val, int len, Type... types) {
    this.val = val;
    this.len = len;
    this.types = types;
  }

  public static Instruction fromValue(int val) {
    // search for equivalent enum type
    for (Instruction test : values()) {
      if (test.val == val) {
        return test;
      }
    }
    // default return if lookup failed
    return NO_MSG;
  }

  public Type getType(int idx) {
    return types[idx];
  }

  public int toValue() {
    return val;
  }

  public enum Type {

    OBJECT,
    INTEGER,
    LONG,
    STRING;
  }
}
