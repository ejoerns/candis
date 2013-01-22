package candis.distributed.parameter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserParameterSet {
	private final Map<String, UserParameter> mParameters = new HashMap<String, UserParameter>();

	public void AddParameter(UserParameter param) {
		param.mUserParameterValidator.setParameterSet(this);
		mParameters.put(param.getName(), param);

	}
}
