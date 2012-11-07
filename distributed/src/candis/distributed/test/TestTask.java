/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.distributed.IDistributedParameter;
import candis.distributed.IDistributedResult;
import candis.distributed.IDistributedTask;

/**
 *
 * @author swillenborg
 */
public class TestTask implements IDistributedTask {

	@Override
	public IDistributedResult run(IDistributedParameter parameter) {
		TestParameter p = (TestParameter)parameter;
		//throw new UnsupportedOperationException("Not supported yet.");
		return new TestResult(p.number*p.number);
	}

	@Override
	public void stop() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}



}
