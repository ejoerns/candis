package candis.distributed.parameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Sebastian Willenborg
 */
public class ListValidator extends UserParameterValidator {

	final List<String> mValues;
	private String message = "";

	public ListValidator(String[] values) {
		mValues = new LinkedList<String>(Arrays.asList(values));
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean validate(UserParameter param) {
		boolean contains = mValues.contains((String) param.getValue());
		if (contains) {
			message = "";
		}
		else {
			message = String.format("%s is not allowed", param.getValue());
		}
		return contains;

	}
}
