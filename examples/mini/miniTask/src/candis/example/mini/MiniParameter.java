package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniParameter extends DistributedParameter implements Serializable {

	public final int foo;
	public final float bar;

	public MiniParameter(final int foo, final float bar) {
		this.foo = foo;
		this.bar = bar;
	}
}
