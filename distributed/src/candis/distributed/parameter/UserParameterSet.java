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
		if(param.mUserParameterValidator != null){
			param.mUserParameterValidator.setParameterSet(this);
		}
		mParameters.put(param.getName(), param);
	}

	public Iterator<UserParameter> iterator() {
		return mParameters.values().iterator();
	}


	}
