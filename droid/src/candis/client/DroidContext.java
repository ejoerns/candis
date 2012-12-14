package candis.client;

import candis.common.RandomID;
import candis.distributed.droid.StaticProfile;

/**
 * Holds data associated with the droid.
 *
 * @todo Singleton?
 * @author Enrico Joerns
 */
public class DroidContext {

	private RandomID mID;
	private StaticProfile mProfile;

	public DroidContext(RandomID id, StaticProfile profile) {
		mID = id;
		mProfile = profile;
	}

	public RandomID getID() {
		return mID;
	}

	public StaticProfile getProfile() {
		return mProfile;
	}
}
