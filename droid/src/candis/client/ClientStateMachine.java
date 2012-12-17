package candis.client;

import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import candis.client.comm.SecureConnection;
import candis.client.service.BackgroundService;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.DistributedTask;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public final class ClientStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private final DroidContext mDroitContext;
	private final SecureConnection mSConn;
	private final Context mContext;
	private final NotificationManager mNotificationManager;

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
		JOB_FINISHED,
		DISCONNECT;
	}

	private enum ClientHandlerID implements HandlerID {

		MY_ID;
	}

	public ClientStateMachine(
					SecureConnection sconn,
					final DroidContext dcontext,
					final Context context,
					final FragmentManager fragmanager) {
		mDroitContext = dcontext;
		mContext = context;
		mSConn = sconn;

		mNotificationManager =
						(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

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
		addGlobalTransition(
						ClientTrans.DISCONNECT,
						ClientStates.UNCONNECTED,
						new DisconnectHandler());
		setState(ClientStates.UNCONNECTED);
	}

	/**
	 * Shows Error Dialog with short message.
	 */
	private class ConnectionRejectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ConnectionRejectedHandler() called");
			Notification noti = new Notification.Builder(mContext)
							.setContentTitle("Connection Rejected")
							.setContentText("Server rejected connection")
							.setSmallIcon(R.drawable.ic_launcher)
							.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
							.build();
			mNotificationManager.notify(42, noti);
		}
	}

	/**
	 * Sends disconnect instruction to master.
	 */
	private class DisconnectHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("DisconnectHandler() called");
			mSConn.send(new Message(Instruction.DISCONNECT));
		}
	}

	/**
	 * Requests connection to server.
	 */
	private class SocketConnectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("SocketConnectedHandler() called");
			mSConn.send(new Message(Instruction.REQUEST_CONNECTION, mDroitContext.getID()));
		}
	}

	/**
	 * Sends Profile data to Server.
	 */
	private class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ProfileRequestHandler() called");
			mSConn.send(new Message(Instruction.SEND_PROFILE, mDroitContext.getProfile()));
		}
	}

	/**
	 * Shows input dialog to enter checkcode.
	 */
	private class CheckcodeInputHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("CheckcodeInputHandler() called");
			Intent newintent = new Intent(mContext, MainActivity.class);
			newintent.setAction(BackgroundService.SHOW_CHECKCODE)
							.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(newintent);
		}
	}

	/**
	 * Sends entered checkcode to server.
	 */
	private class CheckcodeSendHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			System.out.println("CheckcodeSendHandler() called");
			mSConn.send(new Message(Instruction.SEND_CHECKCODE, (String) o));
		}
	}

	/**
	 *
	 */
	private class BinaryReceivedHandler implements ActionHandler {

		@Override
		public void handle(Object o) {
			LOGGER.log(Level.FINE, "BinaryReceivedHandler() called");

			// create dex class loader
			final File optimizedDexOutputPath = mContext.getDir("outdex", Context.MODE_PRIVATE);
			final File dexInternalStoragePath = new File(mContext.getFilesDir(), "foo.dex");

			DexClassLoader cl = new DexClassLoader(
							dexInternalStoragePath.getAbsolutePath(),
							optimizedDexOutputPath.getAbsolutePath(),
							null,
							this.getClass().getClassLoader());

			// load all available classes
			String path = dexInternalStoragePath.getPath();
			try {
				DexFile dx = DexFile.loadDex(
								path,
								File.createTempFile("opt", "dex", mContext.getCacheDir()).getPath(),
								0);
				for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
					String className = classNames.nextElement();
					try {
						Class<?> aClass = cl.loadClass(className);
						LOGGER.log(Level.INFO, String.format("Loaded class: %s", className));
						// find out which one is the control class
						if (DistributedTask.class.isAssignableFrom(aClass)) {
							System.out.println("Control task is: " + className);
							try {
								aClass.newInstance();
								System.out.println("Created newInstance!");
							}
							catch (InstantiationException ex) {
								Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
							}
							catch (IllegalAccessException ex) {
								Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
							}
						}
					}
					catch (ClassNotFoundException ex) {
						Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
			catch (IOException e) {
				System.out.println("Error opening " + path);
			}

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
			mSConn.send(new Message(Instruction.SEND_RESULT, null));
		}
	}
}
