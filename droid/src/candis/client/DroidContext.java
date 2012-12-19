package candis.client;

import candis.common.DroidID;
import candis.distributed.droid.StaticProfile;
import java.io.Serializable;

/**
 * Holds data associated with the droid.
 *
 * Implemented as singleton.
 *
 * @todo Singleton?
 * @author Enrico Joerns
 */
public class DroidContext implements Serializable {

	private DroidID mID;
	private StaticProfile mProfile;
	private static DroidContext mInstance = null;

	protected DroidContext() {
	}

	public static DroidContext getInstance() {
		if (mInstance == null) {
			mInstance = new DroidContext();
		}
		return mInstance;
	}

	public void init(final DroidContext dcontext) {
		mID = dcontext.mID;
		mProfile = dcontext.mProfile;
	}

	public void setID(final DroidID rid) {
		mID = rid;
	}

	public DroidID getID() {
		return mID;
	}

	public void setProfile(final StaticProfile profile) {
		mProfile = profile;
	}

	public StaticProfile getProfile() {
		return mProfile;
	}
}
