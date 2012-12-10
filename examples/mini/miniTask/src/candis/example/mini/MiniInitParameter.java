package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniInitParameter extends DistributedParameter implements Serializable {
	public final int offset;

	public MiniInitParameter(final int offset) {
		this.offset = offset;
	}

}
