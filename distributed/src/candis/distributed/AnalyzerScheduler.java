package candis.distributed;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Enrico Joerns
 */
public class AnalyzerScheduler extends SimpleScheduler {

  private Map<String, List<Long>> mExecutionTimes = new HashMap<String, List<Long>>();
  private Map<String, Integer> mProcessedParameters = new HashMap<String, Integer>();
  private List<TestData> mData = null;
  long start, fulltime;

  public AnalyzerScheduler(DistributedControl control, int parametersPerJob, boolean multicore) {
    super(control, parametersPerJob, multicore);
  }

  public AnalyzerScheduler(DistributedControl control) {
    super(control);
  }

  @Override
  public void start() {
    start = System.currentTimeMillis();
    super.start();
  }

  @Override
  public void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO) {
    super.schedule(droidList, jobDistIO);

  }

  @Override
  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime) {
    super.onJobDone(droidID, jobID, results, exectime);
    if (!mExecutionTimes.containsKey(droidID)) {
      mExecutionTimes.put(droidID, new LinkedList<Long>());
      mProcessedParameters.put(droidID, 0);
    }
    mExecutionTimes.get(droidID).add(exectime);
    int pcnt = mProcessedParameters.remove(droidID) + results.length;
    mProcessedParameters.put(droidID, pcnt);
  }

  @Override
  public boolean isDone() {
    boolean done = super.isDone();

    if (done && (mData == null)) {
      mData = new LinkedList<TestData>();
      fulltime = System.currentTimeMillis() - start;

      try {
        // Create file 
        FileWriter fstream = new FileWriter("/home/enrico/out.txt");
        System.out.println("********* Writing to: " + "/home/enrico/out.txt");
        BufferedWriter out = new BufferedWriter(fstream);
        FileWriter fstream2 = new FileWriter("/home/enrico/out2.txt");
        System.out.println("********* Writing to: " + "/home/enrico/out2.txt");
        BufferedWriter out2 = new BufferedWriter(fstream2);

        for (Map.Entry<String, List<Long>> times : mExecutionTimes.entrySet()) {
          out2.write(times.getKey().substring(0, 9) + "  ");
          TestData data = new TestData();
          data.id = times.getKey();
          data.jobs = times.getValue().size();
          data.params = mProcessedParameters.get(times.getKey());
          long calctime = 0;
          for (long time : times.getValue()) {
            out2.write(time + "  ");
            calctime += time;
          }
          out2.write("\n");
          data.tCalc = calctime;
          data.tParamAvg = calctime / data.params;
          data.tJobAvg = calctime / data.jobs;
          mData.add(data);
        }

        //
        out.write("ID         jobs     params    tCalc   tJobAvg   tParamAvg\n");
        System.out.println("mData size" + mData.size());
        long fullcalc = 0;
        for (TestData data : mData) {
          out.write(String.format(
                  "%s\t%d\t%d\t%d\t%d\t%d\n",
                  data.id.substring(0, 9),
                  data.jobs,
                  data.params,
                  data.tCalc,
                  data.tJobAvg,
                  data.tParamAvg));
          fullcalc += data.tCalc;
        }
        out.write("t_task: " + fulltime + "\n");
        out.write("t_calc: " + fullcalc + "\n");

        //Close the output streams
        out.close();
        out2.close();
      }
      catch (Exception e) {//Catch exception if any
        System.err.println("Error: " + e.getMessage());
      }
    }
    return done;
  }

  @Override
  public void abort() {
    super.abort();
    System.out.println("*** ABORT!!!");
  }

  private class TestData {

    public String id;
    public int jobs;
    public int params;
    public long tCalc;
    public long tParamAvg;
    public long tJobAvg;
  }
}
