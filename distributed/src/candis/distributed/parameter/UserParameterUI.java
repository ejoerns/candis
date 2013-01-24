package candis.distributed.parameter;

/**
 * Interface for an UI-Element which is capable of showing an Dialog to
 * edit/input some parameters
 *
 * @author Sebastian Willenborg
 */
public interface UserParameterUI {
	/**
	 * Requests to show an Dialog to edit the given parameters
	 *
	 * @param parameterSet UserParameterSet which should be edited
	 */
	void showParameterUIDialog(UserParameterSet parameterSet);
}
