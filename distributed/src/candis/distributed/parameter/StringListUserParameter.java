/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.parameter;

import candis.distributed.parameter.UserParameterCtrl.InputType;

/**
 *
 * @author Sebastian Willenborg
 */
public class StringListUserParameter  extends UserParameter {
	private final String[] mValues;

	public StringListUserParameter(String name, int defaultIndex, String[] values) {
		super(name, values[defaultIndex], new ListValidator(values));
		mValues = values;
	}

	@Override
	public UserParameterCtrl getInputCtrl() {
		return new UserParameterCtrl(InputType.STRING_LIST, mValues);
	}

}
