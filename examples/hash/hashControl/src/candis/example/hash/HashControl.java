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
import candis.distributed.parameter.BooleanUserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import candis.distributed.parameter.StringListUserParameter;
import candis.distributed.parameter.StringUserParameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashControl implements DistributedControl, ResultReceiver {

	@Override
	public Scheduler initScheduler() {
		UserParameterSet parameters = new UserParameterSet();

		StringUserParameter hashvalue = new StringUserParameter("hash.hashvalue", "Hash", "Hash to Crack", "ab12", new RegexValidator("[0-9a-f]*"));
		parameters.AddParameter(hashvalue);

		StringListUserParameter type = new StringListUserParameter("hash.type", 1, new String[] {"md5", "sha1"});
		parameters.AddParameter(type);

		StringListUserParameter tryAlpha = new StringListUserParameter("hash.try.alpha", 2, new String[] {"small", "caps", "both"});
		parameters.AddParameter(tryAlpha);

		BooleanUserParameter tryNumeric = new BooleanUserParameter("hash.try.numeric", false);
		parameters.AddParameter(tryNumeric);

		//parameters.AddParameter(new IntegerUserParameter("hash.trylen.start", 2, 0, Integer.MAX_VALUE, null));
		UserParameterRequester.getInstance().request(parameters);

		System.out.println("hash.hashvalue " + hashvalue.getValue());
		System.out.println("hash.type " + type.getValue());
		System.out.println("try.alpha " + tryAlpha.getValue());
		System.out.println("try.numeric " + tryNumeric.getBooleanValue() + " " + tryNumeric.getValue());
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
