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
public class FSM {

	private class TransitionContainer {
		final StateEnum dest;
		final ActionHandler act;

		public TransitionContainer(StateEnum dest, ActionHandler act) {
			this.dest = dest;
			this.act = act;
		}
	}
	private static final String TAG = "FSM";
	private static final Logger LOGGER = Logger.getLogger("FSM");
	private AtomicReference<StateEnum> mCurrentState = new AtomicReference<StateEnum>();
	/**
	 * Holds all attached states.
	 */
	private final Map<StateEnum, State> mStateMap = new HashMap<StateEnum, State>();
	private final Map<Transition, TransitionContainer> mGloabalTransitions = new HashMap<Transition, TransitionContainer>();

	public FSM() {
	}

	/**
	 *
	 * @param s
	 * @return
	 */
	public final State addState(final StateEnum s) {
		State state = new State(s);
		for(Transition t: mGloabalTransitions.keySet()) {
			TransitionContainer c = mGloabalTransitions.get(t);
			state.addTransition(t, c.dest, c.act);
		}
		mStateMap.put(s, state);
		return state;
	}

	public void addGlobalTransition(Transition trans, StateEnum dest) {
		addGlobalTransition(trans, dest, null);
	}

	public void addGlobalTransition(Transition trans, StateEnum dest, ActionHandler act) {
		for(StateEnum s: mStateMap.keySet()) {
			State st = mStateMap.get(s);
			if(!st.containsTransition(trans)) {
				st.addTransition(trans, dest, act);
			}
		}
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
		// Check if transition is empty
		if (trans == null) {
			LOGGER.log(Level.FINER, "Empty transition");
			return;
		}
		// Check, if the FSM is in an undefined state
		if (mCurrentState.get() == null) {
			throw new NullPointerException("No state set");
		}

		if (!mStateMap.containsKey(mCurrentState.get()) && mStateMap.get(mCurrentState.get()).containsTransition(trans)) {
			// Transition not defined for current State
			throw new StateMachineException(mCurrentState.get(), trans);
		}

		mStateMap.get(mCurrentState.get()).process(trans, obj, mCurrentState);
	}

	public final void process(final Transition trans) throws StateMachineException {
		process(trans, null);
	}
}
