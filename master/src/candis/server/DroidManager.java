package candis.server;

import candis.common.ByteArray;
import candis.common.RandomID;
import candis.distributed.droid.StaticProfile;
import java.util.HashMap;
import java.util.Map;

/**
 * Designed to manage all Droids that ever connected to the Master.
 *
 * The Class is designed as a Singleton.
 *
 * @author Enrico Joerns
 */
public final class DroidManager {

	/// Singleton-Instance
	private static DroidManager instance = new DroidManager();
	/**
	 * List of known droids true means whitelisted, false means blacklisted.
	 */
	private final Map<ByteArray, DroidData> knownDroids = new HashMap<ByteArray, DroidData>();

	/**
	 * Hidden to match Singleton requirements.
	 */
	private DroidManager() {
	}

	/**
	 * Returns Singleton-Instance of DroidManager.
	 *
	 * @return Singleton-Instance of DroidManager instance
	 */
	public static DroidManager getInstance() {
		return instance;
	}

	/**
	 * Adds a Droid to the list of (whitelisted) known Droids.
	 *
	 * @param rid ID of Droid to add
	 */
	public void addDroid(final ByteArray rid, StaticProfile profile) {
		if (!knownDroids.containsKey(rid)) {
			synchronized (knownDroids) {
				knownDroids.put(rid, new DroidData(true, profile));
			}
		}
	}

	/**
	 * Adds a Droid to the list of (whitelisted) known Droids.
	 *
	 * @param rid ID of Droid to add
	 */
	public void addDroid(final RandomID rid, StaticProfile profile) {
		addDroid(new ByteArray(rid.getBytes()), profile);
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final ByteArray rid) {
		if (knownDroids.containsKey(rid)) {
			synchronized (knownDroids) {
				knownDroids.get(rid).setWhitelisted(false);
			}
		}
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final RandomID rid) {
		blacklistDroid(new ByteArray(rid.getBytes()));
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is knonw, otherwise false
	 */
	public boolean isDroidKnown(final ByteArray rid) {
		if (knownDroids.containsKey(rid)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is knonw, otherwise false
	 */
	public boolean isDroidKnown(final RandomID rid) {
		return isDroidKnown(new ByteArray(rid.getBytes()));
	}

	/**
	 * Check if Droid is blacklisted.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is blacklisted, otherwise false (also if unknown)
	 */
	public boolean isDroidBlacklisted(final ByteArray rid) {
		if (!knownDroids.containsKey(rid)) {
			return false;
		}
		if (knownDroids.get(rid).isWhitelisted()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check if Droid is blacklisted.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is blacklisted, otherwise false (also if unknown)
	 */
	public boolean isDroidBlacklisted(final RandomID rid) {
		return isDroidBlacklisted(new ByteArray(rid.getBytes()));
	}

	public StaticProfile getStaticProfile(final ByteArray barray) {
		if (!knownDroids.containsKey(barray)) {
			return null;
		}

		return knownDroids.get(barray).getStaticProfile();
	}

	/**
	 * Class to hold droid data.
	 *
	 * - Blacklisted/Whitelisted
	 *
	 * - Static profile
	 */
	private class DroidData {

		private boolean whitelisted;
		private StaticProfile profile;

		public DroidData(boolean whitelisted, StaticProfile profile) {
			this.whitelisted = whitelisted;
			this.profile = profile;
		}

		public void setWhitelisted(boolean white) {
			this.whitelisted = white;
		}

		public boolean isWhitelisted() {
			return this.whitelisted;
		}

		public StaticProfile getStaticProfile() {
			return this.profile;
		}
	}
}
