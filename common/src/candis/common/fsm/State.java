package candis.common.fsm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class State {

	private static final Logger LOGGER = Logger.getLogger(State.class.getName());
	private final StateEnum name;
	private final Map<Transition, StateEnum> mTransitionMap = new HashMap<Transition, StateEnum>();
	/**
	 * Map of Transitions wiht List of listeners for registered transition.
	 */
	Map<Transition, List<ActionHandler>> mListeners = new HashMap<Transition, List<ActionHandler>>();

	public State(final StateEnum s) {
		name = s;
	}

	public State addTransition(
					final Transition trans,
					final StateEnum dest,
					final ActionHandler act) {
		mTransitionMap.put(trans, dest);
		if (act != null) {
			addActionHandler(trans, act);
		}
		return this;
	}

	public State addTransition(
					final Transition trans,
					final StateEnum dest) {
		return addTransition(trans, dest, null);
	}

	public boolean containsTransition(final Transition trans) {
		return mTransitionMap.containsKey(trans);
	}

	/**
	 * Returns destination state for transition.
	 *
	 * @param trans Transition
	 * @return Resulting state
	 */
	public StateEnum getDestState(final Transition trans) {
		return mTransitionMap.get(trans);
	}

	/**
	 * Processes transition.
	 *
	 * @param trans Transition
	 * @return Resulting state
	 */
	void process(Transition trans, AtomicReference<StateEnum> mCurrentState, Object... obj) throws StateMachineException {

		//String nametrans = trans.toString();
		Logger.getLogger("FSM").log(Level.FINE, String.format(
						"FSM: State: %s, Transition: %s, Object: %s", mCurrentState.get(), trans, obj));

		if (!mTransitionMap.containsKey(trans)) {
			throw new StateMachineException(name, trans);
		}
		else {
			mCurrentState.set(mTransitionMap.get(trans));
			LOGGER.log(Level.FINE, String.format("Updated state to %s", mCurrentState.toString()));
		}
		Logger.getLogger("FSM").log(Level.FINE, String.format(
						"FSM: New State: %s", mCurrentState.get()));

		notifyListeners(trans, obj);
	}

	/**
	 * Adds a listener to the specified transition.
	 *
	 * @param trans
	 * @param listener
	 */
	public void addActionHandler(Transition trans, ActionHandler listener) {
		if (!mListeners.containsKey(trans)) {
			mListeners.put(trans, new LinkedList<ActionHandler>());
		}
		mListeners.get(trans).add(listener);
	}// TODO: concept

	/**
	 * Notifies all listeners for specified transition.
	 *
	 * @param trans Transition to notify for
	 */
	private void notifyListeners(Transition trans, Object... obj) {
		if (mListeners.containsKey(trans)) {
			for (ActionHandler l : mListeners.get(trans)) {
				l.handle(obj);// TODO
			}
		}
	}
}
