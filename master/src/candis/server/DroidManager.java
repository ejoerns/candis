package candis.server;

import candis.common.DroidID;
import candis.common.Utilities;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private static final String TAG = DroidManager.class.getName();
	private static final Logger LOGGER = Logger.getLogger(TAG);
	/// Singleton-Instance
	private static DroidManager instance = new DroidManager();
	private boolean mCheckcodeEnabled = false;
	/**
	 * Map of known droids. true means whitelisted, false means blacklisted.
	 * 'Static' list that will be saved to file.
	 */
	private Map<String, DroidData> mKnownDroids = new ConcurrentHashMap<String, DroidData>();
	/**
	 * Map of connected droids with corresponding handler. Dynamic list that will
	 * be generated at runtime.
	 */
	private Map<String, DroidHandler> mRegisteredDroids = new ConcurrentHashMap<String, DroidHandler>();
	/**
	 * Holds Droids that are currently registering but not yet accepted. (e.g.
	 * checkcode required)
	 */
	private Map<String, DroidHandler> mPendingDroids = new ConcurrentHashMap<String, DroidHandler>();
	private Map<String, StaticProfile> mPendingDroidData = new ConcurrentHashMap<String, StaticProfile>();
	/**
	 * Holds all pending (send but not verified) checkcodes.
	 */
	private Map<String, String> mPendingCheckCodes = new ConcurrentHashMap<String, String>();
	/**
	 * List of connected Manager Listeners expecting changes of DroidStates.
	 */
	private final List<DroidManagerListener> mListeners = new LinkedList<DroidManagerListener>();
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
		return mKnownDroids;
	}

	/**
	 * Returns the list of connected Droids.
	 *
	 * @return Map of connected Droids.
	 */
	public Set<String> getRegisteredDroids() {
		return mRegisteredDroids.keySet();
	}

	/**
	 * Invoked to communicate with specific droid.
	 */
	public interface DroidHandler {

		public void onSendJob(String taskID, String jobID, DistributedJobParameter[] params);

		public void onStopJob(String taskID, String jobID);

		public void onAccepted();

		public void onRejected();

		/// Droid needs to send checkcode
		public void onRequireCheckcode();
	}

	public void enableCheckcodeInput(boolean value) {
		mCheckcodeEnabled = value;
	}

	/**
	 * A new connection(FSM) calls this with its droid ID to try to register at
	 * the DroidManager.
	 *
	 * If it is accepted an event is passed to the given handler and further
	 * events for this droid are also passed to this handler.
	 *
	 * @param droidID
	 * @param handler
	 */
	public void register(String droidID, StaticProfile profile, DroidHandler handler) {
		LOGGER.log(Level.INFO, "Droid {0} registering...", droidID);

		// if not known, get known to it
		if (!mKnownDroids.containsKey(droidID) && mCheckcodeEnabled) {

			mPendingDroids.put(droidID, handler);
			mPendingDroidData.put(droidID, profile);
			handler.onRequireCheckcode();
			// notify (gui) listeners to show the checkcode
			notifyListeners(DroidManagerEvent.CHECK_CODE, droidID);
			return;
		}

		// if known and blacklisted, reject it
		if (mKnownDroids.containsKey(droidID) && mKnownDroids.get(droidID).getBlacklist()) {
			LOGGER.log(Level.INFO, "Droid {0} rejected", droidID);
			handler.onRejected();
		}

		// otherwise accept it and add it to list of known if necessary
		if (!mKnownDroids.containsKey(droidID)) {
			LOGGER.log(Level.INFO, "Droid {0}... added", droidID.substring(0, 9));
			mKnownDroids.put(droidID, new DroidData(false, profile));
			notifyListeners(DroidManagerEvent.DROID_ADDED, droidID);
		}

		mRegisteredDroids.put(droidID, handler);
		LOGGER.log(Level.INFO, "Droid {0}... accepted", droidID.substring(0, 9));
		// notify droid about acceptance
		notifyListeners(DroidManagerEvent.DROID_CONNECTED, droidID);
		mRegisteredDroids.get(droidID).onAccepted();
	}

	/**
	 * Disconnects droid from droid manager.
	 *
	 * @param droidID ID of droid that is disconnected
	 */
	public void unregister(final String droidID) {
		LOGGER.log(Level.INFO, "Droid {0} unregistered", droidID);
		mRegisteredDroids.remove(droidID);
		notifyListeners(DroidManagerEvent.DROID_DISCONNECTED, droidID);
	}

	/**
	 * Disconnects droid from droid manager.
	 *
	 * @param rid ID of droid that is disconnected
	 */
	public void unregister(final DroidID rid) {
		unregister(rid.toString());
	}

	/**
	 * Returns the DroidHandler for the given droid ID.
	 *
	 * @param droidID
	 * @return
	 */
	public DroidHandler getDroidHandler(String droidID) {
		if (!mRegisteredDroids.containsKey(droidID)) {
			return null;
		}

		return mRegisteredDroids.get(droidID);
	}

	/**
	 * Generates a checkcode that must be verified with the client.
	 *
	 * @param droidID ID of droid to generate code for
	 * @return Generated code.
	 */
	private String generateCheckcode(String droidID) {
		mPendingCheckCodes.remove(droidID);
		final SecureRandom random = new SecureRandom();
		final byte[] byteCode = new byte[3];
		random.nextBytes(byteCode);

		StringBuffer buf = new StringBuffer();
		int len = byteCode.length;
		for (int i = 0; i < len; i++) {
			Utilities.byte2hex(byteCode[i], buf);
		}
		String code = buf.toString();
		mPendingCheckCodes.put(droidID, code);
		return code;
	}

	/**
	 * Verifies the given code with the stored one and calls pending droid
	 * handler.
	 *
	 * @param droidID ID of droid to verify for
	 * @param code Code to verify
	 */
	public void verifyCheckcode(String droidID, String code) {
		// if droid not pending, forget...
		if (!mPendingDroids.containsKey(droidID)) {
			mPendingCheckCodes.remove(droidID);
			return;
		}

		// check if code available and correct
		if (mPendingCheckCodes.containsKey(droidID) && (mPendingCheckCodes.get(droidID).equals(code))) {
			// make pending ones registered and known ones
			mKnownDroids.put(droidID, new DroidData(mPendingDroidData.get(droidID)));
			mRegisteredDroids.put(droidID, mPendingDroids.get(droidID));
			mPendingDroids.get(droidID).onAccepted();
		}
		else {
			mPendingDroids.get(droidID).onRejected();
		}
		// clear pending lists
		mPendingDroids.remove(droidID);
		mPendingDroidData.remove(droidID);
		mPendingCheckCodes.remove(droidID);
	}

	/**
	 * Removes a Droid from the list of known Droids.
	 *
	 * @param droidID ID of Droid to remove
	 */
	public void deleteDroid(final String droidID) {
		if (mKnownDroids.containsKey(droidID)) {
			mKnownDroids.remove(droidID);
			notifyListeners(DroidManagerEvent.DROID_DELETED, droidID);
		}
		if (mRegisteredDroids.containsKey(droidID)) {
			LOGGER.log(Level.INFO, "Droid {0} deleted", droidID);
			mRegisteredDroids.remove(droidID);
//			mRegisteredDroids.get(droidID).onDisconnect();
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
		if (mKnownDroids.containsKey(droidID)) {
			mKnownDroids.get(droidID).setBlacklist(true);
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
		if (mKnownDroids.containsKey(droidID)) {
			mKnownDroids.get(droidID).setBlacklist(false);
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
		return mKnownDroids.containsKey(rid);
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
		return mRegisteredDroids.containsKey(rid);
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
		if (!mKnownDroids.containsKey(rid)) {
			return false;
		}
		return mKnownDroids.get(rid).getBlacklist();
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
		if (!mKnownDroids.containsKey(droidID)) {
			return null;
		}

		return mKnownDroids.get(droidID).getProfile();
	}

	/**
	 * Loads droid database form xml file.
	 *
	 * @param file XML file to load from
	 * @throws FileNotFoundException File not found
	 */
	public void load(final File file) throws FileNotFoundException {
		mKnownDroids = readFromXMLFile(file);
		notifyListeners();
	}

	/**
	 * Stores droid database to xml file.
	 *
	 * @param file XML file to store to
	 * @throws FileNotFoundException File not found
	 */
	public void store(final File file) throws FileNotFoundException {
		writeToXMLFile(file, mKnownDroids);
	}

	/**
	 * Initialize droid manager database.
	 */
	public void init() {
		mKnownDroids = new ConcurrentHashMap<String, DroidData>(8, 0.9f, 1);
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
		for (DroidManagerListener d : mListeners) {
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
		mListeners.add(listener);
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
		if (!mPendingCheckCodes.containsKey(droidID)) {
			return "";
		}
		return mPendingCheckCodes.get(droidID);
	}
}
