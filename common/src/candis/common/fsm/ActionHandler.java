package candis.common.fsm;

/**
 *
 * @author Enrico Joerns
 */
public interface ActionHandler {

	/**
	 *
	 * @param obj
	 */
	void handle(Object... obj);
}
