package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniInitParameter extends DistributedParameter implements Serializable {
	public int offset;

	public MiniInitParameter(int offset) {
		this.offset = offset;
	}

}
