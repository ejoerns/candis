package candis.common.fsm;

import candis.common.CandisLog;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a state for the FSM.
 *
 * It holds all associated Transitions and ActionHanlers.
 *
 * @author Enrico Joerns
 */
public class State {

  private static final String TAG = State.class.getName();
  private final StateEnum name;
  /// Map of Transitions with its target States.
  private final Map<Transition, StateEnum> mTransitionMap = new HashMap<Transition, StateEnum>();
  /// Map of Transitions wiht List of listeners for registered transition.
  Map<Transition, List<ActionHandler>> mListeners = new HashMap<Transition, List<ActionHandler>>();

  /**
   * Creates State from given StateEnum.
   *
   * @param s
   */
  public State(final StateEnum s) {
    name = s;
  }

  /**
   * Adds a transition to this state.
   *
   * @param trans Transition to add
   * @param dest Target state
   * @param act ActionHandler to execut
   * @return Returns itself to allow concatenation of multiple addTransition()
   * commands to ease FSM creation.
   */
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
    CandisLog.v(TAG, String.format(
            "FSM: State: %s, Transition: %s, Object: %s", mCurrentState.get(), trans, obj));

    if (!mTransitionMap.containsKey(trans)) {
      throw new StateMachineException(name, trans);
    }
    else {
      mCurrentState.set(mTransitionMap.get(trans));
      CandisLog.d(TAG, String.format("Updated state to %s", mCurrentState.toString()));
    }
    CandisLog.d(TAG, String.format("FSM: New State: %s", mCurrentState.get()));

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
