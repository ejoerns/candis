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
  long start, fulltime;

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
    }
    mExecutionTimes.get(droidID).add(exectime);
  }

  @Override
  public boolean isDone() {
    boolean done = super.isDone();

    if (done) {
      fulltime = System.currentTimeMillis() - start;

      try {
        // Create file 
        FileWriter fstream = new FileWriter("/home/enrico/out.txt");
        System.out.println("********* Writing to: " + "/home/enrico/out.txt");
        BufferedWriter out = new BufferedWriter(fstream);

        out.write("t_task: " + fulltime + "\n");
        for (Map.Entry<String, List<Long>> times : mExecutionTimes.entrySet()) {
          out.write(times.getKey() + "\n");
          out.write("jobs:   " + times.getValue().size() + "\n");
          long avg = 0;
          for (long time : times.getValue()) {
            avg += time;
          }
          avg /= times.getValue().size();
          out.write("t_avg:  " + avg + "\n");
        }
        //Close the output stream
        out.close();
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
}
