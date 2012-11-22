package candis.common.fsm;

import android.util.Log;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
// class FSM<? extends> {...
public class FSM {

	private static final String TAG = "FSM";
	private static Logger LOGGER = Logger.getLogger("FSM");
	private AtomicReference<StateEnum> mCurrentState = new AtomicReference<StateEnum>();
	/**
	 * Holds all attached states.
	 */
	private final Map<StateEnum, State> mStateMap = new HashMap<StateEnum, State>();

	public FSM() {
		System.setProperty("candis.client.logging", "FINEST");
		System.setProperty("java.util.logging.config.file", "logging.properties");
	}

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
		mCurrentState.set(state);
	}

	/**
	 * Returns current mCurrentState of FSM.
	 *
	 * @return Current mCurrentState
	 */
	public final StateEnum getState() {
		return mCurrentState.get();
	}

	/**
	 * Processes transition.
	 *
	 * @param trans Transition to process
	 * @throws StateMachineException Something went wrong
	 */
	public final void process(final Transition trans, Object obj) throws StateMachineException {
		LOGGER.log(Level.FINE, String.format(
						"process() Trans: %s, Obj: %s", trans, obj));
		if (trans == null) {
			LOGGER.log(Level.FINER, "Empty Transition");
			return;
		}
		if (!mStateMap.containsKey(mCurrentState.get())) {
			throw new StateMachineException(); // State undefined
		}
//		final StateEnum newState = mStateMap.get(mCurrentState).process(trans, obj);
		mStateMap.get(mCurrentState.get()).process(trans, obj, mCurrentState);
//		if (newState != null) {
//			mCurrentState = newState;
//		}
	}

	public final void process(final Transition trans) throws StateMachineException {
		process(trans, null);
	}
}
