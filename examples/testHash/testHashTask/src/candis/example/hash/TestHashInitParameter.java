package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable initial parameter for "global" settings.
 */
public class TestHashInitParameter implements DistributedJobParameter {

	public final String string;
          
	public TestHashInitParameter(String s) {
    string = s;
	}
}
