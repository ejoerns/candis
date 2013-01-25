package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class UserParameter {

	protected Object mValue;
	protected final String mName;
	protected final String mDescription;
	protected final UserParameterValidator mUserParameterValidator;
	protected final String mTitle;

	public UserParameter(String name, Object defaultValue, UserParameterValidator validator) {
		this(name, null, "", defaultValue, validator);
	}

	public UserParameter(String name, String title, String description, Object defaultValue, UserParameterValidator validator) {
		mValue = defaultValue;
		mName = name;
		mTitle = title;
		mDescription = description;
		mUserParameterValidator = validator;
	}

	public void SetValue(Object value) {
		this.mValue = value;
	}

	public Object getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	public String getTitle() {
		if (mTitle != null) {
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
