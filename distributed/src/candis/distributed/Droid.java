package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
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
