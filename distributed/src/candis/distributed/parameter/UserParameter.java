package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class UserParameter {

	protected Object data;
	protected final String mName;
	protected final String mDescription;
	protected final UserParameterValidator mUserParameterValidator;
	protected final String mTitle;

	public UserParameter(String name, Object defaultValue, UserParameterValidator validator) {
		this(name, null, "", defaultValue, validator);
	}

	public UserParameter(String name, String title, String description, Object defaultValue, UserParameterValidator validator) {
		data = defaultValue;
		mName = name;
		mTitle = title;
		mDescription = description;
		mUserParameterValidator = validator;
	}

	public void SetData(Object data) {
		this.data = data;
	}

	public Object getData() {
		return data;
	}

	public String getName() {
		return mName;
	}
	public String getTitle() {
		if(mTitle != null) {
			return mTitle;
		}
		return mName;
	}

	public String getDescription() {
		return mDescription;
	}

	public boolean validate() {
		if (mUserParameterValidator == null) {
			return true;
		}
		return mUserParameterValidator.validate(this);
	}

	public String getValidatorMessage() {
		if (mUserParameterValidator == null) {
			return "";
		}
		return mUserParameterValidator.getMessage();
	}

	public abstract UserParameterCtrl getInputCtrl();
}
