package candis.system;

import java.math.BigInteger;

/**
 *
 * @author Enrico Joerns
 */
public class FakBenchmark extends Benchmark {

	private static final int MAX_FAK = 30000;

	@Override
	public void run() {
		//-- BigInteger solution.
		BigInteger n = BigInteger.ONE;
		for (int i = 1; i <= MAX_FAK; i++) {
			n = n.multiply(BigInteger.valueOf(i));
//			System.out.println(i + "! = " + n);
		}
	}
}
