package candis.distributed.parameter;

import java.io.File;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserParameterRequester {
	static UserParameterRequester instance = null;

	private UserParameterRequester(boolean cli, File configfile) {

	}

	public static void init() {
		init(false, null);
	}

	public static void init(boolean cli, File config) {
		if(instance != null) {
			instance = new UserParameterRequester(cli, config);
		}
	}

	public static UserParameterRequester getInstance() {
		return instance;
	}

	public void request(UserParameterSet parameters) {
		
	}
}
