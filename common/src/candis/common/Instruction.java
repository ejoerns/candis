/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.common;

import candis.common.fsm.Transition;

/**
 * Message enum type.
 *
 * Serialize: CandisMsg mymessage = CandisMsg.GET_INFO; int simpleVal =
 * mymessage.toValue();
 *
 * Deserialize: CandisMsg newmessage = CandisMsg.fromValue(simpleVal);
 *
 * @author enrico
 */
public enum Instruction implements Transition {

	// -- Available messages
	NO_MSG(0),
	/// Request information - unused
	GET_INFO(10),
	/// Send information - unused
	SEND_INFO(20),
	/// Droid requests connection to master
	REQUEST_CONNECTION(40),
	// Master requests ID - unused
	//	REQUEST_ID(50),
	/// Master requests profile data from droid
	REQUEST_PROFILE(60),
	/// Droid sends profile data
	SEND_PROFILE(65),
	/// Master accepts connection
	ACCEPT_CONNECTION(70),
	/// Master rejects connection
	REJECT_CONNECTION(80),
	/// Droid requests job
	REQUEST_JOB(90),
	/// Master sends job
	SEND_JOB(100),
	/// Droid sends result to master
	SEND_RESULT(105),
	/// Droid/Master informs that it will terminate
	SELF_TERMINATE(110);
	// --
	private final int val;

	Instruction(int val) {
		this.val = val;
	}

	public static Instruction fromValue(int val) {
		// search for equivalent enum type
		for (Instruction test : values()) {
			if (test.val == val) {
				return test;
			}
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
