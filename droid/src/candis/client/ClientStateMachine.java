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
import candis.client.gui.CheckcodeInputDialog;
import candis.client.gui.ErrorMessageDialog;
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
//	private ObjectOutputStream mOutStream = null;
	private final RandomID mRid;
	private final StaticProfile mProfile;
	private final Handler mHandler;
	private final FragmentManager mFragManager;
	private final SecureConnection mSConn;

	private enum ClientStates implements StateEnum {

		UNCONNECTED,
		WAIT_ACCEPT,
		CHECKCODE_ENTER,
		CHECKCODE_SENT,
		PROFILE_SENT,
		CONNECTED,
		JOB_RECEIVED, BINARY_RECEIVED, INIT_RECEIVED;
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
		mSConn = sconn;
//		try {
//			this.mOutStream = new ObjectOutputStream(sconn.getOutputStream());
//		}
//		catch (IOException ex) {
//			LOGGER.log(Level.SEVERE, null, ex);
//		}
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
						ClientStates.CONNECTED,
						null)
						.addTransition(
						Instruction.REJECT_CONNECTION,
						ClientStates.UNCONNECTED,
						new ConnectionRejectedHandler())
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
						ClientStates.CONNECTED,
						null);
		addState(ClientStates.CONNECTED)
						.addTransition(
						Instruction.SEND_BINARY,
						ClientStates.BINARY_RECEIVED,
						new BinaryReceivedHandler());
		addState(ClientStates.BINARY_RECEIVED)
						.addTransition(
						Instruction.SEND_INITAL,
						ClientStates.INIT_RECEIVED,
						new InitialParameterReceivedHandler())
						.addTransition(
						Instruction.SEND_BINARY,
						ClientStates.BINARY_RECEIVED,
						new BinaryReceivedHandler());
		addState(ClientStates.INIT_RECEIVED)
						.addTransition(
						Instruction.SEND_JOB,
						ClientStates.JOB_RECEIVED,
						new JobReceivedHandler())
						.addTransition(
						Instruction.SEND_BINARY,
						ClientStates.BINARY_RECEIVED,
						new BinaryReceivedHandler());
		addState(ClientStates.JOB_RECEIVED)
						.addTransition(
						ClientTrans.JOB_FINISHED,
						ClientStates.INIT_RECEIVED,
						new JobFinishedHandler());
		setState(ClientStates.UNCONNECTED);
	}

	/**
	 * Shows Error Dialog with short message.
	 */
	private class ConnectionRejectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ConnectionRejectedHandler() called");
			mHandler.post(new Runnable() {
				public void run() {
					DialogFragment checkDialog = new ErrorMessageDialog("Connection refused!");
					checkDialog.show(mFragManager, TAG);
				}
			});
		}
	}

	/**
	 * Requests connection to server.
	 */
	private class SocketConnectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("SocketConnectedHandler() called");
//			try {
//				// todo: ID
//				mOutStream.writeObject(new Message(Instruction.REQUEST_CONNECTION, mRid));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.REQUEST_CONNECTION, mRid));
		}
	}

	/**
	 * Sends Profile data to Server.
	 */
	private class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ProfileRequestHandler() called");
//			try {
//				mOutStream.writeObject(new Message(Instruction.SEND_PROFILE, mProfile));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.SEND_PROFILE, mProfile));
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
//			try {
//				if (o instanceof String) {
//					System.out.println("Checkcode seems to be: " + (String) o);
//					mOutStream.writeObject(new Message(Instruction.SEND_CHECKCODE, (String) o));
//				}
//				else {
//					throw new ClassCastException("Data failure, expected String, got Trash");
//				}
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.SEND_CHECKCODE, (String) o));
		}
	}

	/**
	 *
	 */
	private class BinaryReceivedHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("BinaryReceivedHandler() called");
			// TODO...
//			try {
//				mOutStream.writeObject(new Message(Instruction.ACK));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.ACK));
		}
	}

	/**
	 *
	 */
	private class InitialParameterReceivedHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("InitialParameterReceivedHandler() called");
			// TODO...
//			try {
//				mOutStream.writeObject(new Message(Instruction.ACK));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.ACK));
		}
	}

	/**
	 *
	 */
	private class JobReceivedHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("JobReceivedHandler() called");
			// TODO...
//			try {
//				mOutStream.writeObject(new Message(Instruction.ACK));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.ACK));
		}
	}

	/**
	 *
	 */
	private class JobFinishedHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("CheckcodeSendHandler() called");
			// TODO...
//			try {
//				mOutStream.writeObject(new Message(Instruction.SEND_RESULT, null));
//			}
//			catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
//			}
			mSConn.send(new Message(Instruction.SEND_RESULT, null));
		}
	}
}
