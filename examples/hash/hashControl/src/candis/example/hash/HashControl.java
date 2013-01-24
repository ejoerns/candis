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
import candis.distributed.parameter.InvalidUserParameterException;
import candis.distributed.parameter.RegexValidator;
import candis.distributed.parameter.UserBooleanParameter;
import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import candis.distributed.parameter.UserStringListParameter;
import candis.distributed.parameter.UserStringParameter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashControl implements DistributedControl, ResultReceiver {

	@Override
	public Scheduler initScheduler() {
		UserParameterSet parameters = new UserParameterSet();
		parameters.AddParameter(new UserStringParameter("hash.hashvalue", "Hash", "Hash to Crack", "ab12", new RegexValidator("[0-9a-f]*")));
		parameters.AddParameter(new UserStringListParameter("hash.type", 1, new String[] {"md5", "sha1"}));
		parameters.AddParameter(new UserStringListParameter("hash.try.alpha", 2, new String[] {"small", "caps", "both"}));
		parameters.AddParameter(new UserBooleanParameter("hash.try.numeric", false));
		//parameters.AddParameter(new UserIntegerParameter("hash.trylen.start", 2, 0, Integer.MAX_VALUE, null));
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
