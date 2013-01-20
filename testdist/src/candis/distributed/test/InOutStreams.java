package candis.distributed.test;

import candis.server.CDBLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Helperclass to generate an Object based Inter-Thread Stream.
 *
 * @author Sebastian Willenborg
 */
public class InOutStreams {

	private final ObjectInputStream in;
	private final ObjectOutputStream out;
	private final List<Integer> buffer = new LinkedList<Integer>();
	private final CDBLoader mCDBLoader;

	/**
	 * Getter for ObjectInputStream.
	 *
	 * @return The created InputStream
	 */
	public ObjectInputStream getInputStream() {
		return in;
	}

	/**
	 * Getter for ObjectOutputStream.
	 *
	 * @return The created OutputStream
	 */
	public ObjectOutputStream getOutputStream() {
		return out;
	}

	/**
	 * Initializes a new Pair of Input and Output Streams.
	 *
	 * @throws IOException
	 */
	public InOutStreams(CDBLoader CDBLoader) throws IOException {

		mCDBLoader = CDBLoader;
		// Create new OutputStream writing to buffer
		out = new ObjectOutputStream(new OutputStream() {
			@Override
			public void write(int i) throws IOException {
				synchronized (buffer) {
					buffer.add(i);
					buffer.notify();
				}
			}
		});

		// Create new InputStream reading from buffer
		// If you are reading this comment, please skip the next 30 lines
		in = new ObjectInputStream(new InputStream() {
			@Override
			public int read() throws IOException {
				try {
					synchronized (buffer) {
						while (buffer.isEmpty()) {
							buffer.wait();
						}
						return buffer.remove(0);
					}
				}
				catch (InterruptedException ex) {
					throw new InterruptedIOException();
				}
			}
		})
		{
			@Override
			public Class resolveClass(ObjectStreamClass desc) throws IOException,
						ClassNotFoundException {

			try {
				return mCDBLoader.getClassLoaderWrapper().get().loadClass(desc.getName());
			}
			catch (Exception e) {
				return super.resolveClass(desc);
			}
		}
		};
	}
}
