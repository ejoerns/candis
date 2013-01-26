package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserParameterCtrl {

	private final InputType mInputType;
	private final String[] mListElements;

	public enum InputType {

		INTEGER,
		STRING,
		STRING_LIST,
		FILE,
		BOOELAN;
	};

	public UserParameterCtrl(InputType type) {
		this(type, null);
	}

	public UserParameterCtrl(InputType type, String[] listElements) {
		mInputType = type;
		mListElements = listElements;
	}

	public InputType getInputTupe() {
		return mInputType;
	}

	public boolean hasList() {
		return mListElements != null;
	}

	public String[] getListElements() {
		return mListElements;
	}
}
