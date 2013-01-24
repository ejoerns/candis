package candis.distributed.parameter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserParameterSet implements Iterable<UserParameter> {
	private final Map<String, UserParameter> mParameters = new LinkedHashMap<String, UserParameter>();


	public void AddParameter(UserParameter param) {
		param.mUserParameterValidator.setParameterSet(this);
		mParameters.put(param.getName(), param);
		//mNames.add(param.getName());
	}

	public Iterator<UserParameter> iterator() {
		return mParameters.values().iterator();
	}


	}
