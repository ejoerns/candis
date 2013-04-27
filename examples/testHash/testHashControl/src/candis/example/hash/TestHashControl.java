package candis.example.hash;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.parameter.IntegerUserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import java.util.Stack;
import java.util.UUID;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestHashControl extends DistributedControl implements ResultReceiver {

  public String mResultValue;
  private TestHashInitParameter init;
  private Stack<TestHashJobParameter> parameters = new Stack<TestHashJobParameter>();

  @Override
  public void init() {
    UserParameterSet uparams = new UserParameterSet();
    IntegerUserParameter jobs = new IntegerUserParameter("hash.trylen.start", "Parameter", "Specify the minimal length of the brutefoce string",
                                                         1000, 1, Integer.MAX_VALUE, 1, null);
    uparams.AddParameter(jobs);

    UserParameterRequester.getInstance().request(uparams);

    init = new TestHashInitParameter(UUID.randomUUID().toString());

    for (int i = 0; i < jobs.getIntegerValue(); i++) {
      parameters.add(new TestHashJobParameter(UUID.randomUUID().toString()));
    }
  }

  @Override
  public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
  }

  @Override
  public final void onSchedulerDone() {
  }

  @Override
  public final DistributedJobParameter getParameter() {
    return parameters.pop();
  }

  @Override
  public final long getParametersLeft() {
    return parameters.size();
  }

  @Override
  public final boolean hasParametersLeft() {
    return !parameters.isEmpty();
  }

  @Override
  public DistributedJobParameter getInitialParameter() {
    return init;
  }
}
