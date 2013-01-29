package candis.common.fsm;

/**
 *
 * @author Enrico Joerns
 */
public abstract class ActionHandler {

	/**
	 *
	 * @param obj
	 */
	public abstract void handle(Object... obj);
	private static final boolean DEBUG = false;

	protected void gotCalled() {
		if (!DEBUG) {
			return;
		}
		System.out.println(String.format("%s() called", this.getClass().getSimpleName()));
	}
}
