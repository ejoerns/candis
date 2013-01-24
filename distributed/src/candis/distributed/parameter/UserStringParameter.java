package candis.distributed.parameter;

import candis.distributed.parameter.UserParameterCtrl.InputType;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserStringParameter extends UserParameter {
	public UserStringParameter(String name, String defaultValue, UserParameterValidator validator) {
		super(name, defaultValue, validator);
	}

	public UserStringParameter(String name, String title, String description, String defaultValue, UserParameterValidator validator) {
		super(name, title, description, defaultValue, validator);
	}

	@Override
	public UserParameterCtrl getInputCtrl() {
		return new UserParameterCtrl(InputType.STRING);
	}
}
