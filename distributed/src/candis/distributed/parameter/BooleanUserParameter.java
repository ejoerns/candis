package candis.distributed.parameter;

import candis.distributed.parameter.UserParameterCtrl.InputType;

/**
 *
 * @author Sebastian Willenborg
 */
public class BooleanUserParameter extends UserParameter {

	public BooleanUserParameter(String name, boolean defaultValue) {
		super(name, defaultValue, null);
	}

	public BooleanUserParameter(String name, boolean defaultValue, UserParameterValidator validator) {
		super(name, defaultValue, validator);
	}

	public boolean getBooleanValue() {
		return Boolean.parseBoolean(mValue.toString());
	}

	@Override
	public UserParameterCtrl getInputCtrl() {
		return new UserParameterCtrl(InputType.BOOELAN);
	}
}
