/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed;

/**
 *
 * @author swillenborg
 */
public class Droid {

	public boolean equals(Droid other) {
		return false;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Droid) {
			return equals((Droid) other);
		}
		return false;
	}

}
