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
	private String message = "";
	private String regex;
	public RegexValidator(String regex) {
		this.regex = regex;
	}

	public boolean validate(UserParameter param) {
		if(!String.class.isInstance(param.getValue())) {
			message = "the given data is no string";
			return false;
		}
		String data = (String)param.getValue();
		if(!data.matches(regex)) {
			message = String.format("%s didn't match %s", data, regex);
			return false;
		}
		message = "";
		return true;
	}

	public String getMessage() {
		return message;
	}


}
