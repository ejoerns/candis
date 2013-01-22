/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.parameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class UserStringParameter extends UserParameter {
	public UserStringParameter(String name, String defaultValue, UserParameterValidator validator) {
		super(name, defaultValue, validator);
	}
}
