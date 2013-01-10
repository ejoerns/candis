package candis.common.fsm;

import candis.common.CandisLog;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a Finite State Machine 'FSM'.
 *
 * The FSM consists of 3 types: States, Transitions and ActionHandlers.
 *
 * All states should be declared in an enum that implements the StateEnum
 * interface.
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
  private AtomicReference<StateEnum> mCurrentState = new AtomicReference<StateEnum>();
  /// Map of all StateEnums with its corresponding State objects.
  private final Map<StateEnum, State> mStateMap = new HashMap<StateEnum, State>();
  /// Holds all declared Transitions
  private final Map<Transition, TransitionContainer> mGloabalTransitions = new HashMap<Transition, TransitionContainer>();

  public FSM() {
  }

  /**
   * Adds a new state to the FSM.
   *
   * @param s State to add. Must implement interface StateEnum.
   * @return Returns itself to allow adding of transitions directly.
   */
  public final State addState(final StateEnum s) {
    State state = new State(s);
    for (Transition t : mGloabalTransitions.keySet()) {
      TransitionContainer c = mGloabalTransitions.get(t);
      state.addTransition(t, c.dest, c.act);
    }
    mStateMap.put(s, state);
    return state;
  }

  /**
   * Adds a transition that is used for all registered states.
   *
   * The FSM will switch to the declared state if the declared transition
   * is processed.
   *
   * @param trans Transition to add
   * @param dest Target state
   */
  public void addGlobalTransition(Transition trans, StateEnum dest) {
    addGlobalTransition(trans, dest, null);
  }

  /**
   * Adds a transition that is used for all registered states.
   *
   * The FSM will switch to the declared State and execute the declared
   * ActionHandler if the declared Transition is processed.
   *
   * @param trans Transition to add
   * @param dest Target state
   * @param act ActionHandler to execute
   */
  public void addGlobalTransition(Transition trans, StateEnum dest, ActionHandler act) {
    for (StateEnum s : mStateMap.keySet()) {
      State st = mStateMap.get(s);
      if (!st.containsTransition(trans)) {
        st.addTransition(trans, dest, act);
      }
    }
  }

  /**
   * Sets the current of the FSM manually.
   *
   * Should be used at least once to init the FSM with a specific State.
   *
   * @param state State to set
   */
  public final void setState(final StateEnum state) {
    mCurrentState.set(state);
  }

  /**
   * Returns current state of FSM.
   *
   * @return Current state
   */
  public final StateEnum getState() {
    return mCurrentState.get();
  }

  /**
   * Processes transition.
   *
   * Main method to interact with the FSM. Processes incoming Transition
   * and switches to new state depending on current state and its declared
   * Transitions.
   *
   * If an ActionHandler is available for this transition it is executed
   * with the provided Vararg parameter list.
   *
   * @param trans Transition to process
   * @param obj Data payload to provide to a possibly registered ActionHandler
   * @throws StateMachineException Something went wrong
   */
  public final synchronized void process(final Transition trans, Object... obj) throws StateMachineException {
    CandisLog.v(TAG, String.format(
            "process() Trans: %s, Obj: %s", trans, obj));
    // Check if transition is empty
    if (trans == null) {
      CandisLog.v(TAG, "Empty transition");
      return;
    }
    // Check, if the FSM is in an undefined state
    if (mCurrentState.get() == null) {
      throw new NullPointerException("No state set");
    }
    if (mStateMap.get(mCurrentState.get()) == null) {
      throw new NullPointerException(String.format("State %s not known to FSM", mCurrentState.get()));
    }

    if (!mStateMap.containsKey(mCurrentState.get()) && mStateMap.get(mCurrentState.get()).containsTransition(trans)) {
      // Transition not defined for current State
      throw new StateMachineException(mCurrentState.get(), trans);
    }

    mStateMap.get(mCurrentState.get()).process(trans, mCurrentState, obj);
  }

  /**
   * Processes transition.
   *
   * Main method to interact with the FSM. Processes incoming Transition
   * and switches to new state depending on current state and its declared
   * Transitions.
   *
   * @param trans Transition to process
   * @param obj Data payload to provide to a possibly registered ActionHandler
   * @throws StateMachineException Something went wrong
   */
  public final synchronized void process(final Transition trans) throws StateMachineException {
    process(trans, (Serializable) null);
  }
}
