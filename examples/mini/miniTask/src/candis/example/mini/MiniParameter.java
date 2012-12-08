package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 * Serializable task parameter for MiniTask.
 */
public class MiniParameter extends DistributedParameter implements Serializable {

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
	public MiniParameter(final int foo, final float bar) {
		this.foo = foo;
		this.bar = bar;
	}
}