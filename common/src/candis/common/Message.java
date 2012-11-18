package candis.common;

import java.io.Serializable;

/**
 * Data packet that is transmitted between client and server.
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

	public Message(final Instruction req) {
		this(req, null);
	}

	public Instruction getRequest() {
		return req;
	}

	public Serializable getData() {
		return data;
	}
}
