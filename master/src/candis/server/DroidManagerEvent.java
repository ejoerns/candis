package candis.server;

import java.io.Serializable;

/**
 *
 * @author Enrico Joerns
 */
public enum DroidManagerEvent implements Serializable {
	UPDATE,
	DROID_CONNECTED,
	DROID_DISCONNECTED;
}
