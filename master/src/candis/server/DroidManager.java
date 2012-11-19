package candis.server;

import candis.common.RandomID;
import candis.common.Utilities;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

/**
 * Designed to manage all Droids that ever connected to the Master.
 *
 * The Class is designed as a Singleton.
 *
 * @author Enrico Joerns
 */
@XmlRootElement
public final class DroidManager {

	/// Singleton-Instance
	private static DroidManager instance = new DroidManager();
	/**
	 * List of known droids true means whitelisted, false means blacklisted.
	 */
	private final Map<String, DroidData> knownDroids = new HashMap<String, DroidData>();

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
	 * @param profile
	 */
	public void addDroid(final String rid, StaticProfile profile) {
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
	 * @param profile
	 */
	public void addDroid(final RandomID rid, final StaticProfile profile) {
		addDroid(Utilities.toSHA1String(rid.getBytes()), profile);
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final String rid) {
		if (knownDroids.containsKey(rid)) {
			synchronized (knownDroids) {
				knownDroids.get(rid).setBlacklist(true);
			}
		}
	}

	/**
	 * Blacklists a known Droid.
	 *
	 * @param rid Droid to blacklist
	 */
	public void blacklistDroid(final RandomID rid) {
		blacklistDroid(new String(rid.getBytes()));
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is knonw, otherwise false
	 */
	public boolean isDroidKnown(final String rid) {
		if (knownDroids.containsKey(rid)) {
			return true;
		} else {
			return false;
		}
	}

	public Map<String, DroidData> getKnownDroid() {
		return knownDroids;
	}

	/**
	 * Check if Droid is known (either black- or whitelisted).
	 *
	 * @param rid ID of Droid to check
	 * @return true if Droid is knonw, otherwise false
	 */
	public boolean isDroidKnown(final RandomID rid) {
		return isDroidKnown(new String(rid.getBytes()));
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
	public boolean isDroidBlacklisted(final RandomID rid) {
		return isDroidBlacklisted(new String(rid.getBytes()));
	}

	public StaticProfile getStaticProfile(final String barray) {
		if (!knownDroids.containsKey(barray)) {
			return null;
		}

		return knownDroids.get(barray).getProfile();
	}

	/**
	 * Loads droid data set from server.
	 *
	 * @param file xml file to load from
	 * @return
	 */
	public static Map<String, DroidData> readFromXMLFile(final File file) throws FileNotFoundException {
		DroidHashMapType map = null;
		try {

			final JAXBContext jaxbContext = JAXBContext.newInstance(
							DroidHashMapType.class);
			final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			map = (DroidHashMapType) jaxbUnmarshaller.unmarshal(file);

		} catch (JAXBException ex) {
			Logger.getLogger(DroidManager.class.getName()).log(Level.SEVERE, null, ex);
		}
		return map.getHashMap();
	}

	/**
	 * Stores droid data set to xml file.
	 *
	 * @param file filename to store under
	 * @param manager map data to store
	 * @throws FileNotFoundException If file was not found
	 */
	public static void writeToXMLFile(
					final File file,
					final DroidManager manager)
					throws FileNotFoundException {
		try {

			final JAXBContext jaxbContext = JAXBContext.newInstance(
							DroidHashMapType.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			// write to stdout
			jaxbMarshaller.marshal(new DroidHashMapType(manager.getKnownDroid()), System.out);
			jaxbMarshaller.marshal(new DroidHashMapType(manager.getKnownDroid()), file);
			// write to file
		} catch (JAXBException ex) {
			Logger.getLogger(DroidManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
