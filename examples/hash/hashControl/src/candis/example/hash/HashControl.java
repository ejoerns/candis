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
import candis.distributed.parameter.BooleanUserParameter;
import candis.distributed.parameter.IntegerUserParameter;
import candis.distributed.parameter.StringListUserParameter;
import candis.distributed.parameter.StringUserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;

/**
 *
 * @author Sebastian Willenborg
 */
public class HashControl implements DistributedControl, ResultReceiver {

	@Override
	public Scheduler initScheduler() {
		UserParameterSet parameters = new UserParameterSet();
		// 900150983cd24fb0d6963f7d28e17f72 = md5("abc");
		StringUserParameter hashvalue = new StringUserParameter("hash.hashvalue", "Hash (hex)", "Enter hash to crack here", "900150983cd24fb0d6963f7d28e17f72", new HashInputValidator());
		parameters.AddParameter(hashvalue);

		StringListUserParameter type = new StringListUserParameter("hash.type", "Hash-Method", "Specifiy the type of the hash", 0, new String[]{"md5", "sha1"});
		parameters.AddParameter(type);

		StringListUserParameter tryAlpha = new StringListUserParameter("hash.try.alpha", "Characters", "Use small charactes (a-z), caps (A-Z) or both.", 2, new String[]{"small", "caps", "both", "none"});
		parameters.AddParameter(tryAlpha);

		BooleanUserParameter tryNumeric = new BooleanUserParameter("hash.try.numeric", "Numbers", "Use Numbers", false);
		parameters.AddParameter(tryNumeric);

		IntegerUserParameter start = new IntegerUserParameter("hash.trylen.start", "Minimal Length", "Specify the minimal length of the brutefoce string", 2, 1, Integer.MAX_VALUE, 1, new SmallerThanValidator("hash.trylen.stop"));
		parameters.AddParameter(start);

		IntegerUserParameter stop = new IntegerUserParameter("hash.trylen.stop", "Maximal Length", "Specify the maximal length of the bruteforce string", 2, 1, Integer.MAX_VALUE, 1, new BiggerThanValidator("hash.trylen.start"));
		parameters.AddParameter(stop);

		UserParameterRequester.getInstance().request(parameters);

		System.out.println("hashvalue " + hashvalue.getValue());
		System.out.println("type " + type.getValue());
		System.out.println("try.alpha " + tryAlpha.getValue());
		System.out.println("try.numeric " + tryNumeric.getBooleanValue());
		System.out.println("trylen.start " + start.getIntegerValue());
		System.out.println("trylen.stop " + stop.getIntegerValue());
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
