package candis.server;

import java.io.Serializable;

/**
 *
 * @author Enrico Joerns
 */
public enum DroidManagerEvent implements Serializable {
	UPDATE,
	DROID_ADDED,
	DROID_CONNECTED,
	DROID_DISCONNECTED,
	DROID_BLACKLISTED,
	DROID_WHITELISTED;
}
