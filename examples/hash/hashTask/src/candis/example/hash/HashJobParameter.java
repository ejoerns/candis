package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class HashJobParameter implements DistributedJobParameter {

	/// Some value
	public final int foo;
	/// Another value
	public final float bar;

	/**
	 * Initializes the Task Parameters for MiniTask
	 *
	 * @param foo some integer value
	 * @param bar some float value
	 */
	public HashJobParameter(final int foo, final float bar) {
		this.foo = foo;
		this.bar = bar;
	}
}