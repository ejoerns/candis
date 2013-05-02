package candis.client.android;

import candis.client.JobCenter;
import candis.client.TaskProvider;
import candis.distributed.DistributedRunnable;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class AndroidTaskProvider extends TaskProvider {

  private static final Logger LOGGER = Logger.getLogger(AndroidTaskProvider.class.getName());
  private final File mTmpDir;
  
  public AndroidTaskProvider(File tmpDir) {
    super(tmpDir);
    mTmpDir = tmpDir;
  }
  
  @Override
  protected void loadClassesFromJar(File jarfile) throws JobCenter.TaskNotFoundException {
    LOGGER.info("Calling DexClassLoader with jarfile: " + jarfile.getAbsolutePath());
    final File optDexOutDir = new File(mTmpDir, "dex");
    optDexOutDir.mkdirs();
    // set new classloader
    ClassLoader classloader = new DexClassLoader(
            jarfile.getAbsolutePath(),
            optDexOutDir.getAbsolutePath(),
            null,
            JobCenter.class.getClassLoader());
    super.setClassLoader(classloader);

    // load all available classes
    String path = jarfile.getPath();
    try {
      // load dexfile
      DexFile dx = DexFile.loadDex(path, File.createTempFile("opt", "dex", mTmpDir).getPath(), 0);
      // extract all available classes
      for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
        String className = classNames.nextElement();
        LOGGER.info(String.format("found class: %s", className));
        try {
          // TODO: do only forName() here?
          final Class<Object> loadedClass = (Class<Object>) classloader.loadClass(className);
          LOGGER.info(String.format("Loaded class: %s", className));
          // add associated classes to task class list
          if (loadedClass == null) {
            LOGGER.severe("loadedClass is null");
          }
          // add task class to task list
          if (DistributedRunnable.class.isAssignableFrom(loadedClass)) {
            super.setRunnableClass(loadedClass);
          }
        }
        catch (ClassNotFoundException ex) {
          LOGGER.severe(ex.getMessage());
        }
      }
    }
    catch (IOException e) {
      LOGGER.severe(e.getMessage());
      throw new JobCenter.TaskNotFoundException();
    }
  }
}
