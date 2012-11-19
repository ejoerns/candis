package candis.server;

import candis.distributed.droid.StaticProfile;

/**
 * Class to hold droid data.
 *
 * - Blacklisted/Whitelisted
 *
 * - Static profile
 */
class DroidData {

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

	public StaticProfile getProfile() {
		return this.profile;
	}

	public void getProfile(StaticProfile profile) {
		this.profile = profile;
	}
}