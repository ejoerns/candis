package candis.common.fsm;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Enrico Joerns
 */
// class FSM<? extends> {...
public class FSM {

	private StateEnum mCurrentState;
	/**
	 * Holds all attached states.
	 */
	private final Map<StateEnum, State> mStateMap = new HashMap<StateEnum, State>();

	/**
	 *
	 * @param s
	 * @return
	 */
	public final State addState(final StateEnum s) {
		State state = new State(s);
		mStateMap.put(s, state);
		return state;
	}

	/**
	 * Sets the mCurrentState of the FSM.
	 *
	 * @param state State to set
	 */
	public final void setState(final StateEnum state) {
		mCurrentState = state;
	}

	/**
	 * Returns current mCurrentState of FSM.
	 *
	 * @return Current mCurrentState
	 */
	public final StateEnum getState() {
		return mCurrentState;
	}

	/**
	 * Processes transition.
	 *
	 * @param trans Transition to process
	 * @throws StateMachineException Something went wrong
	 */
	public final void process(final Transition trans) throws StateMachineException {
		if (trans == null) {
			return;
		}
		if (!mStateMap.containsKey(mCurrentState)) {
			throw new StateMachineException(); // State undefined
		}
		final StateEnum newState = mStateMap.get(mCurrentState).process(trans);
		if (newState != null) {
			mCurrentState = newState;
		}
	}
}
