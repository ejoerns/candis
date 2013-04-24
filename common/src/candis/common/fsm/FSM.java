package candis.common.fsm;

import candis.common.CandisLog;
import candis.common.WorkerQueue;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

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
public abstract class FSM {

  private class TransitionContainer {

    final StateEnum dest;
    final ActionHandler act;

    public TransitionContainer(StateEnum dest, ActionHandler act) {
      this.dest = dest;
      this.act = act;
    }
  }
  private static final String TAG = "FSM";
  private AtomicReference<StateEnum> mPreviousState = new AtomicReference<StateEnum>();
  private AtomicReference<StateEnum> mCurrentState = new AtomicReference<StateEnum>();
  /// Map of all StateEnums with its corresponding State objects.
  private final Map<StateEnum, State> mStateMap = new HashMap<StateEnum, State>();
  /// Holds all declared Transitions
  private final Map<Transition, TransitionContainer> mGloabalTransitions = new HashMap<Transition, TransitionContainer>();
  private final Map<Transition, StateEnum> mGlobalTransitionMap = new HashMap<Transition, StateEnum>();
  /// Map of Transitions wiht List of listeners for registered transition.
  Map<Transition, List<ActionHandler>> mGlobalListeners = new HashMap<Transition, List<ActionHandler>>();
  /// Fallback state
  private StateEnum mErrorState;
  private ActionHandler mErrorHandler;
  private WorkerQueue mWorkQueue = new WorkerQueue();

  public FSM() {
  }

  /**
   * Must be implemented to init the FSM
   */
  public void init() {
    new Thread(mWorkQueue).start();
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
   * @todo Currently only working if all states are already added!
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
   * Sets a transition that is invoked if a state machine error occurs.
   *
   * @param dest Target state
   * @param act ActionHandler to execute
   */
  public final void setErrorTransition(StateEnum dest, ActionHandler act) {
    mErrorState = dest;
    mErrorHandler = act;
  }

  /**
   * Sets the current of the FSM manually.
   *
   * Should be used at least once to init the FSM with a specific State.
   *
   * @param state State to set
   */
  public final void setState(final StateEnum state) {
    mPreviousState.set(null);
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

  public final StateEnum getPreviousState() {
    return mPreviousState.get();
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
  public final void process(final Transition trans, final Object... obj) throws StateMachineException {
    mWorkQueue.add(new Runnable() {
      public void run() {
        _process(trans, obj);
      }
    });
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
  public final void process(final Transition trans) throws StateMachineException {
    mWorkQueue.add(new Runnable() {
      public void run() {
        _process(trans, (Serializable) null);
      }
    });
  }

  public final void _process(final Transition trans, Object... obj) throws StateMachineException {
    CandisLog.v(TAG, String.format(
            "process() Trans: %s, Obj: %s", trans, obj));
    // Check if transition is empty
    if (trans == null) {
      CandisLog.v(TAG, "Empty transition");
      return;
    }
    // Check, if the FSM is in an undefined state
    if (mCurrentState.get() == null) {
      throw new StateMachineException("No state set. Did you forget to call init()?");
    }
    if (mStateMap.get(mCurrentState.get()) == null) {
      throw new StateMachineException(String.format("State %s not known to FSM", mCurrentState.get()));
    }

    if (!mStateMap.containsKey(mCurrentState.get()) && mStateMap.get(mCurrentState.get()).containsTransition(trans)) {
      // Transition not defined for current State
      throw new StateMachineException(mCurrentState.get(), trans);
    }

    mPreviousState.set(mCurrentState.get());
    try {
      mStateMap.get(mCurrentState.get()).process(trans, mCurrentState, obj);
    }
    catch (StateMachineException ex) {
      if (mErrorState == null) {
        throw ex;
      }
      // do error transition
      Logger.getLogger(FSM.class.getName()).warning(ex.toString());
      setState(mErrorState);
      mErrorHandler.handle(obj);
    }
  }
}
