package candis.common.fsm;

import java.util.HashMap;
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
	private final Map<Transition, ActionHandler> mHandlerMap = new HashMap<Transition, ActionHandler>();

	public State(final StateEnum s) {
		name = s;
	}

	public State addTransition(
					final Transition trans,
					final StateEnum dest,
					final ActionHandler act) {
		mTransitionMap.put(trans, dest);
		mHandlerMap.put(trans, act);
		return this;
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
	public ActionHandler getActionHandler(final Transition trans) {
		return mHandlerMap.get(trans);
	}

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

		if ((mHandlerMap.containsKey(trans)) && (mHandlerMap.get(trans) != null)) {
			mHandlerMap.get(trans).handle(obj);
		}
//		return mCurrentState = process(trans, obj);
	}
}
