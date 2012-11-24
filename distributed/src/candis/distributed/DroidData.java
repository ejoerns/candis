package candis.distributed;

import candis.distributed.droid.StaticProfile;

/**
 * Class (bean) to hold droid data.
 *
 * - Blacklisted/Whitelisted
 *
 * - Static profile
 */
public class DroidData {

	private boolean blacklist;
	private StaticProfile profile;

	public DroidData(boolean blacklist, StaticProfile profile) {
		this.blacklist = blacklist;
		this.profile = profile;
	}

	public void setBlacklist(boolean white) {
		this.blacklist = white;
	}

	public boolean getBlacklist() {
		return this.blacklist;
	}

	public void setProfile(StaticProfile profile) {
		this.profile = profile;
	}

	public StaticProfile getProfile() {
		return this.profile;
	}
}
