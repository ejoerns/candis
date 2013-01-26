package candis.distributed.parameter;

import candis.distributed.parameter.UserParameterCtrl.InputType;

/**
 *
 * @author Sebastian Willenborg
 */
public class IntegerUserParameter extends UserParameter {

	final int mMin;
	final int mMax;
	final int mStep;

	public IntegerUserParameter(String name, int defaultValue, int min, int max, int step, UserParameterValidator validator) {
		super(name, Integer.valueOf(defaultValue), validator);
		mMin = min;
		mMax = max;
		mStep = step;
	}

	@Override
	public UserParameterCtrl getInputCtrl() {
		return new UserParameterCtrl(InputType.INTEGER);
	}

	public int getIngegerValue() {
		return ((Integer) mValue).intValue();
	}

	public int getMax() {
		return mMax;
	}

	public int getMin() {
		return mMin;
	}

	public int getStep() {
		return mStep;
	}
}
