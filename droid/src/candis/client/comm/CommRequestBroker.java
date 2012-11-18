package candis.client.comm;

import android.util.Log;
import candis.client.ClientStateMachine;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class CommRequestBroker implements Runnable {

	private boolean isStopped;
	private FSM fsm;
	private SecureConnection sconn;

//	public CommRequestBroker(
//					final String host,
//					final int port,
//					final X509TrustManager tstore,
//					final FSM fsm) {
//		super(host, port, tstore);
//	}
	public CommRequestBroker(final SecureConnection sconn, final FSM fsm) {
		this.sconn = sconn;
		this.fsm = fsm;
	}

	@Override
	public void run() {
		Log.i("CRB", "run() called");
		sconn.connect();
		try {
			fsm.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
		} catch (StateMachineException ex) {
			Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
		}
		Message m = null;
		while (!isStopped) {
			try {
				Object o = sconn.readObject();
				if (o instanceof Message) {
					Logger.getLogger("CRB").log(Level.INFO, "Read new Message");
					try {
						fsm.process(((Message) o).getRequest());
					} catch (StateMachineException ex) {
						Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException ex) {
					Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
				}
			} catch (IOException ex) {
				Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
