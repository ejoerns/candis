package candis.distributed.droid;

/**
 *
 * @author Sebastian Willenborg
 */
public class Droid {
	public final int id;
	public StaticProfile profile;

	public Droid(int id) {
		this.id = id;
	}

	public boolean equals(Droid other) {
		return this.id == other.id;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Droid) {
			return equals((Droid) other);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + this.id;
		return hash;
	}
}
