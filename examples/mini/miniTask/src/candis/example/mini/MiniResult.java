package candis.example.mini;

import candis.distributed.DistributedResult;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniResult extends DistributedResult implements Serializable {

	public final float foobar;

	public MiniResult(float foobar) {
		this.foobar = foobar;
	}
}
