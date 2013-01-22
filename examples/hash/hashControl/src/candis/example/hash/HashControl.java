/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.example.hash;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.parameter.RegexValidator;
import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import candis.distributed.parameter.UserStringParameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashControl implements DistributedControl, ResultReceiver {

	@Override
	public Scheduler initScheduler() {
		UserParameterSet parameters = new UserParameterSet();
		parameters.AddParameter(new UserStringParameter("hash.hashvalue", "ab12", new RegexValidator("[0-9a-f]*")));
		UserParameterRequester.getInstance().request(parameters);
		//parameters
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onSchedulerDone() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
