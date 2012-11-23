package candis.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Enrico Joerns
 */
public interface DroidManagerListener {

	/**
	 * Called by DroidManager if Droid list updates.
	 *
	 * @param event Indicates type of Event
	 * @param knownDroids List of known Droids
	 * @param connectedDroids List of connected Droids
	 */
	void handle(
					DroidManagerEvent event,
					DroidManager manager);
}
