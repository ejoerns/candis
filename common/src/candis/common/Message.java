/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.common;

import java.io.Serializable;

/**
 *
 * @author enrico
 */
public class Message implements Serializable {
	
	
	private Serializable data;
	private Instruction req;

	public Message(final Instruction req, final Serializable data) {
		this.req = req;
		this.data = data;
	}
	
	public Instruction getRequest() {	
		return req;
	}
	
	public Serializable getData() {
		return data;
	}
	
}
