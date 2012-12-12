package candis.server;

import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DroidData;
import candis.distributed.Scheduler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO, Runnable {

	protected Scheduler scheduler;
	protected DistributedControl distributedControl;
	protected final DroidManager mDroidManager;
	private Thread queueThread;
	private static final Logger LOGGER = Logger.getLogger(ServerCommunicationIO.class.getName());
	private final List<Runnable> comIOQueue = new LinkedList<Runnable>();
	private byte[] mServerBinary;
	private byte[] mDroidBinary;

	public ServerCommunicationIO(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setDistributedControl(DistributedControl dc) {
		if (scheduler == null || scheduler.isDone()) {
			distributedControl = dc;
			scheduler = dc.initScheduler();
			scheduler.setCommunicationIO(this);
			queueThread = new Thread(this);
			queueThread.start();
		}


	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

	public byte[] getDroidBinary() {
		return mDroidBinary;
	}

	public byte[] getServerBinary() {
		return mServerBinary;
	}

	@Override
	public void startJob(String id, DistributedParameter param) {
		Connection d = getDroidConnection(id);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_JOB, param);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void sendBinary(String droidID) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_BINARY, getDroidBinary());
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void sendInitialParameter(String droidID, DistributedParameter parameter) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_INITAL, parameter);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void stopJob(final String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	private boolean isQueueEmpty() {
		synchronized (comIOQueue) {
			return comIOQueue.isEmpty();
		}
	}

	@Override
	public void run() {
		try {
			while (!(isQueueEmpty() && scheduler.isDone())) {

				while (!isQueueEmpty()) {
					Runnable task;
					synchronized (comIOQueue) {
						task = comIOQueue.remove(0);

					}
					task.run();
				}
				if (!scheduler.isDone()) {
					synchronized (comIOQueue) {
						if (comIOQueue.isEmpty()) {
							comIOQueue.wait();
						}
					}
				}

			}
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		distributedControl.schedulerDone();
		LOGGER.log(Level.INFO, "CommunicationIO done");
	}

	@Override
	public void join() throws InterruptedException {
		queueThread.join();
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	protected void addToQueue(Runnable task) {

		synchronized (comIOQueue) {
			comIOQueue.add(task);
			comIOQueue.notify();
		}
	}

	public void startScheduler() {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.start();
			}
		});

	}

	public void onJobDone(final String droidID, final DistributedResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onJobDone(droidID, result);
			}
		});

	}

	public void onDroidConnected(final String droidID, final Connection connection) {
		mDroidManager.connectDroid(droidID, connection);
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onNewDroid(droidID);
			}
		});
	}

	public void onBinarySent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onBinaryRecieved(droidID);
			}
		});
	}

	public void onInitalParameterSent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onInitParameterRecieved(droidID);
			}
		});
	}

	/**
	 * Loads candis distributed bundle.
	 *
	 * File format: zip containing 3 files: - config.properties - droid binary -
	 * server binary
	 *
	 * @param file Name of cdb-file
	 */
	public void loadCandisDistributedBundle(File cdbfile) {
		try {
			ZipFile zipFile = new ZipFile(cdbfile);
			String server_binary = null;
			String droid_binary = null;

			// try to load filenames from properties file
			ZipEntry entry = zipFile.getEntry("config.properties");
			if (entry == null) {
				throw new FileNotFoundException("Zip does not contain 'config.properties'");
			}
			Properties p = new Properties();
			p.load(zipFile.getInputStream(entry));
			server_binary = p.getProperty("server.binary");
			droid_binary = p.getProperty("droid.binary");
			if (server_binary == null) {
				LOGGER.log(Level.SEVERE, "No server binary given");
				return;
			}
			if (droid_binary == null) {
				LOGGER.log(Level.SEVERE, "No droid binary given");
				return;
			}
			// load server binary
			entry = zipFile.getEntry(server_binary);
			mServerBinary = new byte[(int) entry.getSize()];
			zipFile.getInputStream(entry).read(mServerBinary);
			LOGGER.log(Level.FINE, "Loaded server binary", entry.getName());
			// load droid binary
			entry = zipFile.getEntry(droid_binary);
			mDroidBinary = new byte[(int) entry.getSize()];
			zipFile.getInputStream(entry).read(mDroidBinary);
			LOGGER.log(Level.FINE, "Loaded client binary", entry.getName());
		}
		catch (ZipException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}
