package candis.distributed.parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserParameterRequester {

	private static final Logger LOGGER = Logger.getLogger(UserParameterRequester.class.toString());
	static UserParameterRequester instance = null;
	private final boolean mIsCli;
	private final File mConfigFile;
	private final UserParameterUI mUserParameterUI;

	private UserParameterRequester(boolean isCli, File configFile, UserParameterUI ui) {
		mIsCli = isCli;
		mConfigFile = configFile;
		mUserParameterUI = ui;
	}

	public static void init(UserParameterUI ui) {
		init(false, null, ui);
	}

	/**
	 *
	 * @param cli Marks as cli-mode, now doesn't uses ui to get the parameters
	 * @param config
	 */
	public static void init(boolean cli, File config) {
		init(cli, config, null);
	}

	private static void init(boolean cli, File config, UserParameterUI ui) {
		if (instance == null) {
			instance = new UserParameterRequester(cli, config, ui);
		}
	}

	public static UserParameterRequester getInstance() {
		return instance;
	}

	/**
	 * Request to edit/input the given parameters
	 *
	 * @param parameters
	 */
	public void request(UserParameterSet parameters) {
		if (mIsCli) {
			requestCli(parameters);
		}
		else if (mUserParameterUI != null) {
			mUserParameterUI.showParameterUIDialog(parameters);
		}
	}

	private Properties loadConfig() {
		if (mConfigFile == null) {
			return null;
		}
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(mConfigFile));
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return props;
	}

	private void requestCli(UserParameterSet parameters) {
		Properties p = loadConfig();
		for (UserParameter param : parameters) {
			if (p != null && p.containsKey(param.getName())) {
				param.SetValue(p.getProperty(param.getName()));
				LOGGER.log(Level.INFO, "set parameter {0} with value \"{1}\"", new Object[]{param.getName(), param.getValue()});
			}
			else {
				LOGGER.log(Level.WARNING, "missing parameter {0}", param.getName());
			}
		}
		boolean valid = true;
		for (UserParameter param : parameters) {
			if (!param.validate()) {
				LOGGER.log(Level.SEVERE, "invalid parameter {0}: {1}", new Object[]{param.getName(), param.getValidatorMessage()});
				valid = false;
			}
		}
		if (!valid) {
			throw new InvalidUserParameterException();
		}
	}

	private void retquestUi(UserParameterSet parameters) {
		throw new UnsupportedOperationException("UI Not yet implemented");
	}
}
