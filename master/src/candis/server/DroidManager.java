package candis.server;

import candis.common.DroidID;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
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
	private Map<String, ClientConnection> connectedDroids = new ConcurrentHashMap<String, ClientConnection>();
	/**
	 * List of connected Listeners expecting changes of DroidStates.
	 */
	private final List<DroidManagerListener> listeners = new LinkedList<DroidManagerListener>();
	/// stores check code for extended client authorization
	private Map<String, String> mCheckCode = new HashMap<String, String>();
	private Map<String, String> mCheckCodeID = new HashMap<String, String>();

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
	public Map<String, ClientConnection> getConnectedDroids() {
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

	public void addDroid(final String droidID, DroidData droid) {
		if (!knownDroids.containsKey(droidID)) {
			LOGGER.log(Level.INFO, "Droid {0} added", droidID);
			knownDroids.put(droidID, droid);
			notifyListeners(DroidManagerEvent.DROID_ADDED, droidID);
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
	 * @param droidID ID of Droid to remove
	 */
	public void deleteDroid(final String droidID) {
		if (knownDroids.containsKey(droidID)) {
			knownDroids.remove(droidID);
			notifyListeners(DroidManagerEvent.DROID_DELETED, droidID);
		}
		if (connectedDroids.containsKey(droidID)) {
			LOGGER.log(Level.INFO, "Droid {0} deleted", droidID);
			connectedDroids.remove(droidID);
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
	 * @param droidID Droid to blacklist
	 */
	public void blacklistDroid(final String droidID) {
		if (knownDroids.containsKey(droidID)) {
			knownDroids.get(droidID).setBlacklist(true);
			notifyListeners(DroidManagerEvent.DROID_BLACKLISTED, droidID);
		}
		else {
			LOGGER.log(Level.WARNING, "Droid {0} could not be blacklisted", droidID);
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
	 * @param droidID Droid to whitelist
	 */
	public void whitelistDroid(final String droidID) {
		if (knownDroids.containsKey(droidID)) {
			knownDroids.get(droidID).setBlacklist(false);
			notifyListeners(DroidManagerEvent.DROID_WHITELISTED, droidID);
		}
		else {
			LOGGER.log(Level.WARNING, "Droid {0} could not be whitelisted", droidID);
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

	public StaticProfile getStaticProfile(final String droidID) {
		if (!knownDroids.containsKey(droidID)) {
			return null;
		}

		return knownDroids.get(droidID).getProfile();
	}

	/**
	 * Connects droid to droid manager.
	 *
	 * @param droidID
	 */
	public void connectDroid(final String droidID, ClientConnection con) {
		LOGGER.log(Level.INFO, "Droid {0} connected", droidID);
		connectedDroids.put(droidID, con);
		notifyListeners(DroidManagerEvent.DROID_CONNECTED, droidID);
	}

	public void connectDroid(final DroidID rid, ClientConnection con) {
		connectDroid(rid.toString(), con);
	}

	/**
	 * Disconnects droid from droid manager.
	 *
	 * @param droidID ID of droid that is disconnected
	 */
	public void disconnectDroid(final String droidID) {
		LOGGER.log(Level.INFO, "Droid {0} disconnected", droidID);
		connectedDroids.remove(droidID);
		notifyListeners(DroidManagerEvent.DROID_DISCONNECTED, droidID);
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
		notifyListeners();
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
	private void notifyListeners(final DroidManagerEvent event, String droidID) {
		for (DroidManagerListener d : listeners) {
			d.handle(event, droidID, this);
		}
	}

	private void notifyListeners() {
		notifyListeners(DroidManagerEvent.UPDATE, null);
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
	void showCheckCode(final String codeID, final String code, final String droidID) {
		mCheckCodeID.put(droidID, codeID);
		mCheckCode.put(droidID, code);
		notifyListeners(DroidManagerEvent.CHECK_CODE, droidID);
	}

	/**
	 * Validates checkcode and notifies listener to unshow code.
	 *
	 * @param code
	 * @return
	 */
	boolean validateCheckCode(final String code, final String droidID) {
		notifyListeners(DroidManagerEvent.CHECK_CODE_DONE, droidID);
		mCheckCodeID.remove(droidID);
		return mCheckCode.remove(droidID).equals(code);
	}

	/**
	 * Returns check code.
	 *
	 * @return Check code
	 */
	public String getCheckCode(String droidID) {
		return mCheckCode.get(droidID);
	}

	public String getCheckCodeID(String droidID) {
		return mCheckCodeID.get(droidID);
	}

}
