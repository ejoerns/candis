package candis.common.fsm;

/**
 * This interface must be implemented by every state input enum.
 *
 * The Methods are for appending additional data. You need to create a (private)
 * Object reference to hold information and provide access with the defined set
 * and get methods.
 *
 * @author Enrico Joerns
 */
public interface SMInput {

	/**
	 * Accessor method for (private) member variable the implementing enum has to
	 * provide.
	 *
	 * @return Any data to get
	 */
	Object getData();

	/**
	 * Accessor method for (private) member variable the implementing enum has to
	 * provide.
	 *
	 * @param obj Any data to set
	 */
	void setData(final Object obj);
}
