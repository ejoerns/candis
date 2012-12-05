package candis.common.fsm;

/**
 * Dummy Exception class.
 *
 * @author Enrico Joerns
 */
public class StateMachineException extends Exception {

	public StateMachineException(StateEnum currentState, Transition transition) {
		super(String.format("Missing Transition %s.%s in State %s", transition.getClass().getName(), transition.toString(), currentState.toString()));
	}

	public StateMachineException() {
		super();
	}
}
