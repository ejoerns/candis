/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class RegexValidator extends UserParameterValidator{

	public RegexValidator(String regex) {

	}

	public boolean validate(UserParameter param) {
		return false;
	}

	public String getMessage() {
		return "?";
	}


}
