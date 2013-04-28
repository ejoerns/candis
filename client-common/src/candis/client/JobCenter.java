package candis.client;

import android.util.Log;
import candis.common.ClassloaderObjectInputStream;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the execution of tasks.
 *
 * @author Enrico Joerns
 */
public class JobCenter {

  private static final String TAG = JobCenter.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static final int MAX_TASK_CACHE = 5;
  private static final String TASKE_NAME_POSTFIX = ".jar";
  private static final String IPARAM_NAME_POSTFIX = ".par";
  /// maximum number of tasks held in cache
  private final Map<String, TaskFile> mTaskCache = new HashMap<String, TaskFile>();
  private TaskProvider mTaskProvider;
  /// List of all registered handlers
  private final List<JobCenterHandler> mHandlerList = new LinkedList<JobCenterHandler>();
  /// Holds mUsableCores threads for jobs processing.
  private ExecutorService mThreadPool;
  final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
  final Queue<DistributedJobParameter> parameters = new ConcurrentLinkedQueue<DistributedJobParameter>();
  final Object execdone = new Object();
  private int mUsableCores;
  private final File mFilesDir;
  private final File mCacheDir;

  public JobCenter(File filesDir, File cacheDir) {
    mFilesDir = filesDir;
    mCacheDir = cacheDir;
    mUsableCores = DroidContext.getInstance().getProfile().processors;
    mThreadPool = Executors.newFixedThreadPool(mUsableCores);
    mTaskProvider = new TaskProvider(mCacheDir, mTaskCache);
    loadTaskCache();
  }

  /**
   * Enables multicore support.
   *
   * I.e. Number-of-processors threads are started to perform job processing.
   *
   * @param enabled
   */
  public void setMulticore(boolean enabled) {
    if (enabled) {
      mUsableCores = DroidContext.getInstance().getProfile().processors;
    }
    else {
      mUsableCores = 1;
    }
    LOGGER.info(String.format("System will use %d Threads for processing", mUsableCores));
  }

  /**
   * Loads runnables from disk.
   *
   * Note: If more than MAX_TASK_CACHE runnables were found, all
   * remaining runnables will be deleted.
   */
  private void loadTaskCache() {
    // load all available jobs.
    int fileCount = 0;
    for (File taskFile : jarFinder(mFilesDir.getAbsolutePath())) {
      fileCount++;
      String taskID = taskFile.getName().substring(0, taskFile.getName().lastIndexOf('.'));
      LOGGER.info("Found cached task: " + taskID);
      // limit maximum cache files
      if (fileCount > MAX_TASK_CACHE) {
        taskFile.delete();
        LOGGER.info(String.format("Deleted runnable in cache: %s", taskFile.getName()));
      }
      // check for initial parameters
      else if (!new File(taskID.concat(IPARAM_NAME_POSTFIX)).exists()) {
        LOGGER.warning("Saved initial param could not be loaded");
      }
      // if everything is okay, add to current cache map
      else {
        TaskFile file = new TaskFile(mFilesDir, taskID);
        mTaskCache.put(TAG, file);
      }
    }
  }

  /**
   * Looks up all jar files in specified dir.
   *
   * @param dirName Array of File Objects for found jars.
   * @return
   */
  private File[] jarFinder(String dirName) {
    File dir = new File(dirName);

    return dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String filename) {
        return filename.endsWith(".jar");
      }
    });
  }

  /**
   * Adds new runnable (with initial parameter) to runnable cache.
   *
   * @param binary
   */
  public void addRunnable(final String runnableID, final byte[] binary, final byte[] iparam) {

    // If runnable is already in cache, skip
    if (mTaskCache.containsKey(runnableID)) {
      Log.w(TAG, String.format("Warning: Task with ID %s already loaded", runnableID));
      return;
    }

    // store in file and add to cache map
    TaskFile taskFile = new TaskFile(mFilesDir, runnableID);
    taskFile.store(binary, iparam);

    if (mTaskCache.size() >= MAX_TASK_CACHE) {
      // TODO: delte oldest one
    }

    mTaskCache.put(runnableID, taskFile);

    // notify listeners
    for (JobCenterHandler handler : mHandlerList) {
      handler.onBinaryReceived(runnableID);
    }

  }

  /**
   * Processes job...
   *
   * May start multiple threads.
   * Calls onJobExecutionStart() at start.
   * Calls onJobExecutionDone() when processing finished.
   * Assures that order of results matches order of parameters.
   *
   * @param runnableID
   * @param jobID
   * @param param Return true if task available, otherwise false
   */
  public void processJob(final String runnableID, final String jobID, byte[] param) {
    if (!mTaskCache.containsKey(runnableID)) {
      // notify handlers about missing binary
      for (JobCenterHandler handler : mHandlerList) {
        System.out.println("Notifying handler... " + handler);
        handler.onBinaryRequired(runnableID);
      }
      return;
    }

    final DistributedJobParameter[] params = mTaskProvider.deserializeJobParameters(runnableID, param);

    // Check if task can be executed
    if (!checkExecution()) {
      return;
    }

    // notify handlers about start
    for (JobCenterHandler handler : mHandlerList) {
      handler.onJobExecutionStart(runnableID, jobID);
    }

    parameters.clear();
    parameters.addAll(Arrays.asList(params));
    final DistributedJobResult[] results = new DistributedJobResult[parameters.size()];

    new Thread(new Runnable() {
      public void run() {
        long startTime, endTime;
        startTime = System.currentTimeMillis();

        LOGGER.info(String.format("parameters: %d", parameters.size()));
        // start threads
        final int usedThreads = Math.min(mUsableCores, parameters.size());
        // might happen?
        if (usedThreads == 0) {
          LOGGER.warning("Scheduling on 0 Threads, thus canceled");
          for (JobCenterHandler handler : mHandlerList) {
            handler.onJobExecutionDone(null, null, null, 0);
          }
          return;
        }
        LOGGER.info(String.format("usedThreads: %d", usedThreads));
        final int paramsPerThread = (parameters.size() + (usedThreads - 1)) / usedThreads;
        LOGGER.info(String.format("paramsPerThread: %d", paramsPerThread));

        final CountDownLatch latch = new CountDownLatch(usedThreads);

        // start as many threads as allowed
        for (int i = 0; i < usedThreads; i++) {

          mThreadPool.execute(new Runnable() {
            public void run() {
              Log.i(TAG, "Running Thread for Job for Task " + runnableID + " with TaskID " + runnableID);
//              Log.i(TAG, "Threads: " + mThreadPool.getActiveCount());
              DistributedRunnable currentTask;
              try {
                currentTask = mTaskProvider.newRunnableInstance(runnableID);
                currentTask.setInitialParameter(mTaskProvider.getInitialParam(runnableID));
                // execute job for all parameters, assure order
                for (int j = 0; j < paramsPerThread && !parameters.isEmpty(); j++) {
                  DistributedJobParameter runParam;
                  int runID;// needed to assure correct order
                  synchronized (parameters) {
                    runParam = parameters.poll();
                    runID = parameters.size();
                  }
                  DistributedJobResult result = currentTask.execute(runParam);
                  results[results.length - 1 - runID] = result;
                }
                // notify receivers if done
                latch.countDown();
              }
              catch (InstantiationException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
              catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
            }
          });
        }

        System.out.print("Awaiting Threads to finish...");
        try {
          latch.await();
          endTime = System.currentTimeMillis();
          for (JobCenterHandler handler : mHandlerList) {
            handler.onJobExecutionDone(
                    runnableID,
                    jobID,
                    results,
                    endTime - startTime);
          }
        }
        catch (InterruptedException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
        System.out.print("Yeah, they finished!...");
      }
    }).start();

  }

  /**
   * Checks whether Task with specified ID is alread known and loaded.
   *
   * @param runnableID
   * @return true if known, otherwise false
   */
  public boolean isTaskAvailable(String runnableID) {
    return mTaskCache.containsKey(runnableID);
  }

  /**
   * Checks if the execution is ok.
   *
   * @todo Move to FSM level
   * @return
   */
  private boolean checkExecution() {
    return true;
  }

  /**
   *
   * @param dparam
   */
  public boolean setInitialParameter(final String runnableID, final DistributedJobParameter dparam) {
//    Log.i(TAG, "runnableID: " + runnableID);
//    Log.i(TAG, "Param: " + dparam);
//    mTaskProvider.set(runnableID).initialParam = dparam;
//    Log.i(TAG, "Initial Parameter for ID " + runnableID + " loaded with classloader " + ((DistributedJobParameter) dparam).getClass().getClassLoader());
//
//    for (JobCenterHandler handler : mHandlerList) {
//      handler.onInitialParameterReceived(runnableID);
//    }

    return true;
  }

  /**
   *
   * @param handler
   */
  public void addHandler(final JobCenterHandler handler) {
    mHandlerList.add(handler);
  }

  private static class TaskFile {

    private File mRunnable;
    private File mIParam;

    public TaskFile(File cacheDir, String taskID) {
      mRunnable = new File(cacheDir, taskID.concat(TASKE_NAME_POSTFIX));
      mIParam = new File(cacheDir, taskID.concat(IPARAM_NAME_POSTFIX));
    }

    public File getRunnableFile() {
      return mRunnable;
    }

    public File getInitParamFile() {
      return mIParam;
    }

    private void store(byte[] binary, byte[] iparam) {
      writeByteArrayToFile(binary, mRunnable);
      writeByteArrayToFile(iparam, mIParam);
    }

    private static void writeByteArrayToFile(final byte[] data, final File filename) {
      BufferedOutputStream bos = null;

      try {
        //create an object of BufferedOutputStream
        bos = new BufferedOutputStream(new FileOutputStream(filename));
        bos.write(data);
        System.out.println(String.format("File '%s' written", filename));
      }
      catch (FileNotFoundException fnfe) {
        System.out.println("Specified file not found" + fnfe);
      }
      catch (IOException ioe) {
        System.out.println("Error while writing file" + ioe);
      }
      finally {
        if (bos != null) {
          try {
            bos.flush();
            bos.close();
          }
          catch (Exception e) {
          }
        }
      }
    }
  }

  /**
   * Provides access to stored Task files.
   */
  private static class TaskProvider {

    private String mCurrentTaskID;
    /// Holds the runnable Class for Task
    private Class mRunnableClass;
    /// Holds initial parameter for Task
    private DistributedJobParameter mInitialParam;
    /// Holds the Taks classloader
    private ClassLoader mClassLoader;
    /// Directory for tempory data 
    private final File mTmpDir;
    /// Map of all tasks available on disk.
    private final Map<String, TaskFile> mTaskCache;

    /**
     * Creates new TaskProvider.
     *
     * @param tmpDir Directory in which temporary data may be stored.
     * @param taskCache Map of all tasks available on disk.
     */
    public TaskProvider(File tmpDir, Map<String, TaskFile> taskCache) {
      mTmpDir = tmpDir;
      mTaskCache = taskCache;
    }

    /**
     * Returns the initial parameter associated with the specified task.
     *
     * @param taskID ID of task for that the initial parameter should be
     * returned.
     * @return Initial parameter
     */
    public DistributedJobParameter getInitialParam(String taskID) {
      loadTask(taskID);
      return mInitialParam;
    }

    /**
     * Creates new instance of a runnable for specified task.
     *
     * @param taskID ID of task for that the runnable should be created.
     * @return New instance of DistributedRunnable
     * @throws InstantiationException
     * @throws IllegalAccessExceptions
     */
    public DistributedRunnable newRunnableInstance(String taskID) throws InstantiationException, IllegalAccessException {
      loadTask(taskID);
      return (DistributedRunnable) mRunnableClass.newInstance();
    }

    public void setInitialParam(String taskID, DistributedJobParameter iparam) {
      loadTask(taskID);
      mInitialParam = iparam;
    }

    private void loadTask(String taskID) {
      // if already loaded, return without loading
      if (taskID.equals(mCurrentTaskID)) {
        return;
      }
      mCurrentTaskID = taskID;

      loadClassesFromJar(mTaskCache.get(taskID).getRunnableFile());

      // load initial parameter from file
      RandomAccessFile file = null;
      try {
        try {
          file = new RandomAccessFile(mTaskCache.get(taskID).getInitParamFile(), "r");
          byte[] rawdata = new byte[(int) file.length()];
          file.read(rawdata);
          mInitialParam = deserializeJobParameters(taskID, rawdata)[0];
        }
        finally {
          if (file != null) {
            file.close();
          }
        }
      }
      catch (FileNotFoundException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
      catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }

    /**
     * Deserializes the provided serialized DistributedJobParameter
     *
     * @param runnableID runnable ID to deserialize for
     * @return Loaded DistributedJobParameter
     */
    private DistributedJobParameter[] deserializeJobParameters(String taskID, byte[] rawdata) {
      //
      loadTask(taskID);

      //
      ObjectInputStream objInstream;
      Object obj = null;
      try {
        objInstream = new ClassloaderObjectInputStream(
                new ByteArrayInputStream(rawdata),
                mClassLoader);
        obj = objInstream.readObject();
        objInstream.close();
      }
      catch (OptionalDataException ex) {
        Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (ClassNotFoundException ex) {
        Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (IOException ex) {
        Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
      }
      return (DistributedJobParameter[]) obj;
    }

    /**
     * As it name claims, it loads classes from the given jar.
     *
     * @param runnableID
     * @param jarfile
     */
    private void loadClassesFromJar(final File jarfile) {

//      mTaskClasses.clear();

      LOGGER.info("Calling DexClassLoader with jarfile: " + jarfile.getAbsolutePath());
      final File tmpDir = new File(mTmpDir, "dex");
      tmpDir.mkdirs();

      // set new classloader
      mClassLoader = new DexClassLoader(
              jarfile.getAbsolutePath(),
              tmpDir.getAbsolutePath(),
              null,
              JobCenter.class.getClassLoader());

      // load all available classes
      String path = jarfile.getPath();

      try {
        // load dexfile
        DexFile dx = DexFile.loadDex(
                path,
                File.createTempFile("opt", "dex", mTmpDir).getPath(),
                0);

        // extract all available classes
        for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
          String className = classNames.nextElement();
          LOGGER.info(String.format("found class: %s", className));
          try {
            // TODO: do only forName() here?
            final Class<Object> loadedClass = (Class<Object>) mClassLoader.loadClass(className);
            LOGGER.info(String.format("Loaded class: %s", className));
            // add associated classes to task class list
            if (loadedClass == null) {
              LOGGER.severe("loadedClass is null");
            }
//            mTaskClasses.add(loadedClass);
            // add task class to task list
            if (DistributedRunnable.class.isAssignableFrom(loadedClass)) {
              mRunnableClass = loadedClass;
            }
          }
          catch (ClassNotFoundException ex) {
            LOGGER.severe(ex.getMessage());
          }
        }
      }
      catch (IOException e) {
        LOGGER.severe(e.getMessage());
      }
    }
  }
}
