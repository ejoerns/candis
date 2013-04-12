package candis.common.fsm;

/**
 *
 * @author Enrico Joerns
 */
public abstract class ActionHandler {

	/**
	 *
	 * @param data
	 */
	public abstract void handle(Object... data);
	private static final boolean DEBUG = false;

	protected void gotCalled() {
		if (!DEBUG) {
			return;
		}
		System.out.println(String.format("%s() called", this.getClass().getSimpleName()));
	}
}
