 package candis.example.hash;

import candis.distributed.parameter.IntegerUserParameter;
import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterValidator;

/**
 *
 * @author Sebastian Willenborg
 */
class SmallerThanValidator extends UserParameterValidator {

	private String message = "";
	private final String mOther;

	public SmallerThanValidator(String other) {
		mOther = other;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean validate(UserParameter param) {
		IntegerUserParameter other = (IntegerUserParameter) mUserParameterSet.getParameter(mOther);
		IntegerUserParameter me = (IntegerUserParameter) param;
		if (other == null) {
			message = String.format("Parameter \"%s\" not found", mOther);
			return false;
		}
		boolean result = other.getIntegerValue() >= me.getIntegerValue();
		if (result) {
			message = "";
		}
		else {
			message = String.format("This value is bigger than \"%s\"", other.getTitle());
		}
		return result;
	}
}
