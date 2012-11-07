/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.common;

/** Message enum type.
 * 
 * Serialize: 
 *   CandisMsg mymessage = CandisMsg.GET_INFO;  
 *   int simpleVal = mymessage.toValue();
 * 
 * Deserialize:
 *   CandisMsg newmessage = CandisMsg.fromValue(simpleVal);
 *
 * @author enrico
 */
public enum Instruction {
	
	// -- Available messages
	NO_MSG(0),
	GET_INFO(10),
	SEND_INFO(20),
	GET_CERTIFICATE(30),
	SEND_CERTIFICATE(40),
	GET_TRUSTSTORE(50);
	// --

	
	private final int val;
	
	Instruction(int val) {
		this.val = val;
	}
	
	public static Instruction fromValue(int val) {
		// search for equivalent enum type
		for(Instruction test : values()) {
			if (test.val == val) return test;
		}
		// default return if lookup failed
		return NO_MSG;
	}
	
	public int toValue() {
		return val;
	}
	
//	public static final CandisMsg GET_INFO = new CandisMsg("GET_INFO", 10);
//	public static final CandisMsg GET_CERTIFICATE = new CandisMsg("GET_CERT", 20);
//
//	private CandisMsg(final String name, final int val) {
//		this.name = name;
//		this.val = val;
//	}
//	
//	public String getName() {
//		return this.name;
//	}
//	
//	public int getVal() {
//		return this.val;
//	}
	
}
