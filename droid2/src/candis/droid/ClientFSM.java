package candis.droid;

import candis.common.fsm.StateEnum;

/**
 *
 * @author Enrico Joerns
 */
public class ClientFSM {

  private enum ClientStates implements StateEnum {
    REGISTRATING,
    LISTENING,
    JOB_RECEIVED,
    JOB_PROCESSING,
  }
}
