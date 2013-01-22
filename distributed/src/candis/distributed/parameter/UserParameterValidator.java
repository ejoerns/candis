package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class UserParameterValidator {
	protected UserParameterSet mUserParameterSet;

	public abstract String getMessage();
	public abstract boolean validate(UserParameter param);

	public void setParameterSet(UserParameterSet set) {
		mUserParameterSet = set;
	}
}
