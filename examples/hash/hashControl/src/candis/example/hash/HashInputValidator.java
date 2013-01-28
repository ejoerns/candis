/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.example.hash;

import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterValidator;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashInputValidator extends UserParameterValidator {

	private String message = "";

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean validate(UserParameter param) {
		UserParameter type = mUserParameterSet.getParameter("hash.type");
		if (type == null) {
			message = "Parameter \"hash.type\" not found";
			return false;
		}
		String typeString = type.getValue().toString();
		boolean result = false;
		String data = (String) param.getValue();
		if (typeString.equals("md5")) {
			// MD5: 128 bits
			result = data.matches("[0-9a-f]{16}");
		}
		else if (typeString.equals("sha1")) {
			// SHA-1: 160 bits
			result = data.matches("[0-9a-f]{20}");
		}
		else {
			message = String.format("Unknown Hash-Type %s", typeString);
			return false;
		}
		if(result) {
			message = "";
		}
		else {
			message = String.format("Invalid Hash for %s", typeString);
		}
		return result;
	}
}
