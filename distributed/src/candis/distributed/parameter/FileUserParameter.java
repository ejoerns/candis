package candis.distributed.parameter;

import candis.distributed.parameter.UserParameterCtrl.InputType;

/**
 *
 * @author Sebastian Willenborg
 */
public class FileUserParameter extends UserParameter {

	public FileUserParameter(String name, Object defaultValue, UserParameterValidator validator) {
		super(name, defaultValue, validator);
	}

	public FileUserParameter(String name, String title, String description, Object defaultValue, UserParameterValidator validator) {
		super(name, title, description, defaultValue, validator);
	}

	@Override
	public UserParameterCtrl getInputCtrl() {
		return new UserParameterCtrl(InputType.FILE);
	}
}
