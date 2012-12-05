package candis.client;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Handler;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.SecureConnection;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.RandomID;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.droid.StaticProfile;
import candist.client.gui.CheckcodeInputDialog;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public final class ClientStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private ObjectOutputStream mOutStream = null;
	private final RandomID mRid;
	private final StaticProfile mProfile;
	private final Handler mHandler;
	private final FragmentManager mFragManager;

	private enum ClientStates implements StateEnum {

		UNCONNECTED,
		WAIT_ACCEPT,
		CHECKCODE_ENTER,
		CHECKCODE_SENT,
		PROFILE_SENT,
		WAIT_FOR_JOB,
		RECEIVED_JOB;
	}

	public enum ClientTrans implements Transition {

		SOCKET_CONNECTED,
		CHECKCODE_ENTERED,
		JOB_FINISHED;
	}

	private enum ClientHandlerID implements HandlerID {

		MY_ID;
	}

	ClientStateMachine(
					SecureConnection sconn,
					final RandomID rid,
					final StaticProfile profile,
					final Handler handler,
					final FragmentManager manager) {
		this.mRid = rid;
		this.mProfile = profile;
		mHandler = handler;
		mFragManager = manager;
		try {
			this.mOutStream = new ObjectOutputStream(sconn.getOutputStream());
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		init();
	}

	protected void init() {
		addState(ClientStates.UNCONNECTED)
						.addTransition(
						ClientTrans.SOCKET_CONNECTED,
						ClientStates.WAIT_ACCEPT,
						new SocketConnectedHandler());
		addState(ClientStates.WAIT_ACCEPT)
						.addTransition(
						Instruction.REQUEST_PROFILE,
						ClientStates.PROFILE_SENT,
						new ProfileRequestHandler())
						.addTransition(
						Instruction.ACCEPT_CONNECTION,
						ClientStates.WAIT_FOR_JOB,
						null)
						.addTransition(
						Instruction.REQUEST_CHECKCODE,
						ClientStates.CHECKCODE_ENTER,
						new CheckcodeInputHandler());
		addState(ClientStates.CHECKCODE_ENTER)
						.addTransition(
						ClientTrans.CHECKCODE_ENTERED,
						ClientStates.CHECKCODE_SENT,
						new CheckcodeSendHandler());
		addState(ClientStates.CHECKCODE_SENT)
						.addTransition(
						Instruction.REQUEST_PROFILE,
						ClientStates.PROFILE_SENT,
						new ProfileRequestHandler());
		addState(ClientStates.PROFILE_SENT)
						.addTransition(
						Instruction.ACCEPT_CONNECTION,
						ClientStates.WAIT_FOR_JOB,
						null);
		addState(ClientStates.WAIT_FOR_JOB)
						.addTransition(
						Instruction.SEND_JOB,
						ClientStates.RECEIVED_JOB,
						null);
		addState(ClientStates.RECEIVED_JOB)
						.addTransition(
						ClientTrans.JOB_FINISHED,
						ClientStates.WAIT_FOR_JOB,
						null);
		setState(ClientStates.UNCONNECTED);
	}

	/**
	 * Requests connection to server.
	 */
	private class SocketConnectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("SocketConnectedHandler() called");
			try {
				// todo: ID
				mOutStream.writeObject(new Message(Instruction.REQUEST_CONNECTION, mRid));
			}
			catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Sends Profile data to Server.
	 */
	private class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ProfileRequestHandler() called");
			try {
				mOutStream.writeObject(new Message(Instruction.SEND_PROFILE, mProfile));
			}
			catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Shows input dialog to enter checkcode.
	 */
	private class CheckcodeInputHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("CheckcodeInputHandler() called");
			mHandler.post(new Runnable() {
				public void run() {
					DialogFragment checkDialog = new CheckcodeInputDialog(ClientStateMachine.this);
					checkDialog.show(mFragManager, TAG);
				}
			});
		}
	}

	/**
	 * Sends entered checkcode to server.
	 */
	private class CheckcodeSendHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("CheckcodeSendHandler() called");
			try {
				if (o instanceof String) {
					System.out.println("Checkcode seems to be: " + (String) o);
				mOutStream.writeObject(new Message(Instruction.SEND_CHECKCODE, (String) o));
				} else {
					throw new ClassCastException("Data failure, expected String, got Trash");
				}
			}
			catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
