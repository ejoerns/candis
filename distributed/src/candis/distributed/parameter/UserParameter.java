package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class UserParameter {

	protected Object data;
	protected String mName;
	protected final UserParameterValidator mUserParameterValidator;

	public UserParameter(String name, Object defaultValue, UserParameterValidator validator) {
		this.data = defaultValue;
		this.mName = name;
		mUserParameterValidator = validator;
	}

	public void SetData(Object data) {
		this.data = data;
	}

	public Object getData() {
		return this.data;
	}

	public String getName() {
		return this.mName;
	}

	public boolean validate() {
		return this.mUserParameterValidator.validate(this);
	}
}
