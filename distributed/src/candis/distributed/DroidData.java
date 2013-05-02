package candis.distributed;

import candis.distributed.droid.DeviceProfile;

/**
 * Class (bean) to hold droid data.
 *
 * - Blacklisted/Whitelisted
 *
 * - Static profile
 */
public class DroidData {

	private boolean blacklist;
	private DeviceProfile profile;

	public DroidData(boolean blacklist, DeviceProfile profile) {
		this.blacklist = blacklist;
		this.profile = profile;
	}
	
	public DroidData(DeviceProfile profile) {
		this(false, profile);
	}

	public void setBlacklist(boolean black) {
		this.blacklist = black;
	}

	public boolean getBlacklist() {
		return this.blacklist;
	}

	public void setProfile(final DeviceProfile profile) {
		this.profile = profile;
	}

	public DeviceProfile getProfile() {
		return this.profile;
	}
}
