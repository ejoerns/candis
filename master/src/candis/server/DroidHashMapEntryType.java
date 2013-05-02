package candis.server;

import candis.distributed.DroidData;
import candis.distributed.droid.DeviceProfile;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Wrapper class for xml generation with jXAB.
 *
 * @author Enrico Joerns
 */
public class DroidHashMapEntryType {

	@XmlAttribute(name = "blacklisted")
	public boolean blacklisted;
	@XmlAttribute(name = "id-sha1")
	public String key;
	@XmlElement(name = "profile")
	public DeviceProfile value;

	public DroidHashMapEntryType() {
	}

	public DroidHashMapEntryType(Map.Entry<String, DroidData> e) {
		key = e.getKey();
		value = e.getValue().getProfile();
		blacklisted = e.getValue().getBlacklist();
	}
}
