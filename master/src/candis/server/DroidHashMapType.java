/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Enrico Joerns
 */
@XmlRootElement
public class DroidHashMapType {

	@XmlElement(name = "droid")
	public List<DroidHashMapEntryType> entry = new ArrayList<DroidHashMapEntryType>();

	public DroidHashMapType(Map<String, DroidData> map) {
		for (Map.Entry<String, DroidData> e : map.entrySet()) {
			entry.add(new DroidHashMapEntryType(e));
		}
	}

	public DroidHashMapType() {
	}

	public Map<String, DroidData> getHashMap() {
		Map<String, DroidData> map = new HashMap<String, DroidData>();
		for (DroidHashMapEntryType e : entry) {
			map.put(e.key, new DroidData(e.blacklisted, e.value));
		}
		return map;
	}
}
