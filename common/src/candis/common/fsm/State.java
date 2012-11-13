package candis.common.fsm;

/**
 *
 * @author Enrico Joerns
 */
public interface State {

	/**
	 *
	 * @param input Enum that implements pseudo interface SMInput
	 * @return Updated state
	 * @throws StateMachineException If something goes wrong
	 */
	State process(SMInput input) throws StateMachineException;
}
