package candis.server;

import candis.common.DroidID;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Designed to manage all Droids that ever connected to the Master.
 *
 * The Class is designed as a Singleton.
 *
 * @author Enrico Joerns
 */
@XmlRootElement
public final class DroidManager {

	private static final String TAG = "DroidManager";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	/// Singleton-Instance
	private static DroidManager instance = new DroidManager();
	/**
	 * Map of known droids true means whitelisted, false means blacklisted.
	 * 'Static' list that will be saved to file.
	 */
	private Map<String, DroidData> knownDroids = new ConcurrentHashMap<String, DroidData>();
	/**
	 * Map of connected droids with bool flag for further use. Dynamic list that
	 * will be generated at runtime.
	 */
	private Map<String, Connection> connectedDroids = new ConcurrentHashMap<String, Connection>();
	/**
	 * List of connected Listeners expecting changes of DroidStates.
	 */
	private final List<DroidManagerListener> listeners = new LinkedList<DroidManagerListener>();
	/// stores check code for extended client authorization
	private String mCheckCode;

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
	 * Adds a logging handler.
	 *
	 * @param handler
	 */
	public void addLoggerHandler(final Handler handler) {
		LOGGER.addHandler(handler);
	}

	/**
	 * Returns the list of known Droids.
	 *
	 * @return Map of known Droids.
	 */
	public Map<String, DroidData> getKnownDroids() {
		return knownDroids;
	}

	/**
	 * Returns the list of connected Droids.
	 *
	 * @return Map of connected Droids.
	 */
	public Map<String, Connection> getConnectedDroids() {
		return connectedDroids;
	}

	/**
	 * Adds a Droid to the list of (whitelisted) known Droids.
	 *
	 * @param rid ID of Droid to add
	 * @param profile
	 */
	public void addDroid(final String rid, StaticProfile profile) {
		addDroid(rid, new DroidData(false, profile));
	}

	public void addDroid(final String rid, DroidData droid) {
		if (!knownDroids.containsKey(rid)) {
			LOGGER.log(Level.INFO, "Droid {0} added", rid);
			knownDroids.put(rid, droid);
			notifyListeners(DroidManagerEvent.DROID_ADDED);
		}
	}

	/**
	 * Adds a Droid to the list of (whitelisted) known Droids.
	 *
	 * @param rid ID of Droid to add
	 * @param profile
	 */
	public void addDroid(final DroidID rid, final StaticProfile profile) {
		addDroid(rid.toString(), profile);
	}

	/**
	 * Removes a Droid from the list of known Droids.
	 *
	 * @param rid ID of Droid to remove
	 */
	public void deleteDroid(final String rid) {
		if (knownDroids.containsKey(rid)) {
			knownDroids.remove(rid);
			notifyListeners(DroidManagerEvent.DROID_DELETED);
		}
		if (connectedDroids.containsKey(rid)) {
			LOGGER.log(Level.INFO, "Droid {0} deleted", rid);
			connectedDroids.remove(rid);
			// TODO: close connection
		}
	}

	/**
	 * Removes a Droid from the list of known Droids.
	 *
	 * @param rid ID of Droid to remove
	 */
	public void deleteDroid(final DroidID rid) {
		deleteDroid(rid.toString());
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final String rid) {
		if (knownDroids.containsKey(rid)) {
			knownDroids.get(rid).setBlacklist(true);
			notifyListeners(DroidManagerEvent.DROID_BLACKLISTED);
		}
		else {
			LOGGER.log(Level.WARNING, "Droid {0} could not be blacklisted", rid);
		}
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final DroidID rid) {
		blacklistDroid(rid.toString());
	}

	/**
	 * Whitelists a known Droid.
	 *
	 * @param rid Droid to whitelist
	 */
	public void whitelistDroid(final String rid) {
		if (knownDroids.containsKey(rid)) {
			knownDroids.get(rid).setBlacklist(false);
			notifyListeners(DroidManagerEvent.DROID_WHITELISTED);
		}
		else {
			LOGGER.log(Level.WARNING, "Droid {0} could not be whitelisted", rid);
		}
	}

	public void whitelistDroid(final DroidID rid) {
		blacklistDroid(rid.toString());
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is knonw, otherwise false
	 */
	public boolean isDroidKnown(final String rid) {
		return knownDroids.containsKey(rid);
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is known, otherwise false
	 */
	public boolean isDroidKnown(final DroidID rid) {
		return isDroidKnown(rid.toString());
	}

	/**
	 * Returns if droid is connected.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is connected, otherwise false
	 */
	public boolean isDroidConnected(final String rid) {
		return connectedDroids.containsKey(rid);
	}

	/**
	 * Returns if droid is connected.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is connected, otherwise false
	 */
	public boolean isDroidConnected(final DroidID rid) {
		return isDroidConnected(rid.toString());
	}

	/**
	 * Check if Droid is blacklisted.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is blacklisted, otherwise false (also if unknown)
	 */
	public boolean isDroidBlacklisted(final String rid) {
		if (!knownDroids.containsKey(rid)) {
			return false;
		}
		return knownDroids.get(rid).getBlacklist();
	}

	/**
	 * Check if Droid is blacklisted.
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is blacklisted, otherwise false (also if unknown)
	 */
	public boolean isDroidBlacklisted(final DroidID rid) {
		return isDroidBlacklisted(rid.toString());
	}

	public StaticProfile getStaticProfile(final String barray) {
		if (!knownDroids.containsKey(barray)) {
			return null;
		}

		return knownDroids.get(barray).getProfile();
	}

	/**
	 * Connects droid to droid manager.
	 *
	 * @param rid
	 */
	public void connectDroid(final String rid, Connection con) {
		LOGGER.log(Level.INFO, "Droid {0} connected", rid);
		connectedDroids.put(rid, con);
		notifyListeners(DroidManagerEvent.DROID_CONNECTED);
	}

	public void connectDroid(final DroidID rid, Connection con) {
		connectDroid(rid.toString(), con);
	}

	/**
	 * Disconnects droid from droid manager.
	 *
	 * @param rid ID of droid that is disconnected
	 */
	public void disconnectDroid(final String rid) {
		LOGGER.log(Level.INFO, "Droid {0} disconnected", rid);
		connectedDroids.remove(rid);
		notifyListeners(DroidManagerEvent.DROID_DISCONNECTED);
	}

	/**
	 * Disconnects droid from droid manager.
	 *
	 * @param rid ID of droid that is disconnected
	 */
	public void disconnectDroid(final DroidID rid) {
		disconnectDroid(rid.toString());
	}

	/**
	 * Loads droid database form xml file.
	 *
	 * @param file XML file to load from
	 * @throws FileNotFoundException File not found
	 */
	public void load(final File file) throws FileNotFoundException {
		knownDroids = readFromXMLFile(file);
		notifyListeners(DroidManagerEvent.UPDATE);
	}

	/**
	 * Stores droid database to xml file.
	 *
	 * @param file XML file to store to
	 * @throws FileNotFoundException File not found
	 */
	public void store(final File file) throws FileNotFoundException {
		writeToXMLFile(file, knownDroids);
	}

	/**
	 * Initialize droid manager database.
	 */
	public void init() {
		knownDroids = new ConcurrentHashMap<String, DroidData>(8, 0.9f, 1);
	}

	/**
	 * Loads droid data set from server.
	 *
	 * @param file xml file to load from
	 * @return
	 */
	private static Map<String, DroidData> readFromXMLFile(final File file) throws FileNotFoundException {
		DroidHashMapType map = null;
		if (!file.exists()) {
			throw new FileNotFoundException();
		}
		try {

			final JAXBContext jaxbContext = JAXBContext.newInstance(
							DroidHashMapType.class);
			final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			map = (DroidHashMapType) jaxbUnmarshaller.unmarshal(file);

		}
		catch (JAXBException ex) {
			Logger.getLogger(DroidManager.class.getName()).log(Level.SEVERE, null, ex);
		}
		return map.getHashMap();
	}

	/**
	 * Stores droid data set to xml file.
	 *
	 * @param file filename to store under
	 * @param droidmap map data to store
	 * @throws FileNotFoundException If file was not found
	 */
	private static void writeToXMLFile(
					final File file,
					final Map<String, DroidData> droidmap)
					throws FileNotFoundException {
		try {

			final JAXBContext jaxbContext = JAXBContext.newInstance(
							DroidHashMapType.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			// write to file
			jaxbMarshaller.marshal(new DroidHashMapType(droidmap), file);
		}
		catch (JAXBException ex) {
			Logger.getLogger(DroidManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Called to notify all registered listeners.
	 *
	 * @param event Event that should be passed to listeners
	 */
	private void notifyListeners(final DroidManagerEvent event) {
		for (DroidManagerListener d : listeners) {
			d.handle(event, this);
		}
	}

	private void notifyListeners() {
		notifyListeners(DroidManagerEvent.UPDATE);
	}

	/**
	 * Adds a listener that is notifified if anything changes.
	 *
	 * @param listener
	 */
	public void addListener(final DroidManagerListener listener) {
		if (listener == null) {
			return;
		}
		listeners.add(listener);
	}

	/**
	 * Called to show check code.
	 *
	 * Notifies registered listeners with CHECK_CODE event.
	 *
	 * @param code Code do show for theck
	 */
	void showCheckCode(final String code) {
		mCheckCode = code;
		notifyListeners(DroidManagerEvent.CHECK_CODE);
	}

	/**
	 * Validates checkcode and notifies listener to unshow code.
	 *
	 * @param code
	 * @return
	 */
	boolean validateCheckCode(final String code) {
		notifyListeners(DroidManagerEvent.CHECK_CODE_DONE);
		return mCheckCode.equals(code);
	}

	/**
	 * Returns check code.
	 *
	 * @return Check code
	 */
	public String getCheckCode() {
		return mCheckCode;
	}
}
