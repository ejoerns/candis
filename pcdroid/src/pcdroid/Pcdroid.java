package pcdroid;

import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import candis.distributed.parameter.UserParameterUI;
import candis.example.hash.TestHashControl;
import candis.example.hash.TestHashRunnable;

/**
 *
 * @author Enrico Joerns
 */
public class Pcdroid {

  private static final int JOBS = 1000;

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    UserParameterRequester.init(new ParameterDialog());
    TestHashRunnable runnable = new TestHashRunnable();
    TestHashControl control = new TestHashControl();
    control.init();
    long jobs = control.getParametersLeft();
    runnable.setInitialParameter(control.getInitialParameter());

    long start = System.currentTimeMillis();
    for (int i = 0; i < jobs; i++) {
      runnable.execute(control.getParameter());
    }
    long exectime = System.currentTimeMillis() - start;

    System.out.println("Execution took " + exectime + "ms");
  }

  private static class ParameterDialog implements UserParameterUI {

    @Override
    public void showParameterUIDialog(UserParameterSet parameterSet) {
      parameterSet.getParameter("hash.trylen.start").SetValue(JOBS);
    }
  }
}
