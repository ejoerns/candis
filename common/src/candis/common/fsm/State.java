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

	private final StateEnum name;
	private final Map<Transition, StateEnum> mTransitionMap = new HashMap<Transition, StateEnum>();
//	private final Map<Transition, ActionHandler> mHandlerMap = new HashMap<Transition, ActionHandler>();
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
//		mHandlerMap.put(trans, act);
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
	 * Returns action handler for transition.
	 *
	 * @param trans Transition
	 * @return ActionHandler
	 */
//	public ActionHandler getActionHandler(final Transition trans) {
//		return mHandlerMap.get(trans);
//	}
	/**
	 * Processes transition.
	 *
	 * @param trans Transition
	 * @return Resulting state
	 */
//	public StateEnum process(final Transition trans) {
//		return process(trans, null);
//	}
//	public StateEnum process(final Transition trans, final Object obj) {
//		if ((mHandlerMap.containsKey(trans)) && (mHandlerMap.get(trans) != null)) {
//			mHandlerMap.get(trans).handle(obj);
//		}
//
//		if (!mTransitionMap.containsKey(trans)) {
//			return null;
//		}
//		return mTransitionMap.get(trans);
//	}
	void process(Transition trans, Object obj, AtomicReference<StateEnum> mCurrentState) {
		Logger.getLogger("FSM").log(Level.FINE, String.format(
						"FSM: State: %s, Transition: %s, Object: %s", mCurrentState.get(), trans, obj));

		if (!mTransitionMap.containsKey(trans)) {
			mCurrentState.set(null);
		} else {
			mCurrentState.set(mTransitionMap.get(trans));
		}
		Logger.getLogger("FSM").log(Level.FINE, String.format(
						"FSM: New State: %s", mCurrentState.get()));

//		if ((mHandlerMap.containsKey(trans)) && (mHandlerMap.get(trans) != null)) {
//			mHandlerMap.get(trans).handle(obj);
//	}
		notifyListeners(trans, obj);
//		return mCurrentState = process(trans, obj);
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
	private void notifyListeners(Transition trans, Object obj) {
		if (mListeners.containsKey(trans)) {
			for (ActionHandler l : mListeners.get(trans)) {
				l.handle(obj);// TODO
			}
		}
	}
}
