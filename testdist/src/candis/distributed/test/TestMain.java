package candis.distributed.test;

import candis.distributed.SchedulerStillRuningException;
import candis.distributed.parameter.UserParameterRequester;
import candis.server.DroidManager;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain {

	private static final Logger LOGGER = Logger.getLogger(TestMain.class.getName());
	private static final int THREADS_DEFAULT = 4;

	public static void runCDBTest(File cdb, File config, int threads) throws Exception {
		LOGGER.log(Level.INFO, "CDB file {0}", cdb);
		LOGGER.log(Level.INFO, "Config file {0}", config);
		/*File configf = null;
		if (config != null) {
			configf = new File(config);
		}*/
		UserParameterRequester.init(true, config);
		JobDistributionIOTestServer comio = new JobDistributionIOTestServer(DroidManager.getInstance());
		String cdbID = comio.getCDBLoader().loadCDB(cdb);
		comio.initDroids(threads, cdbID);

		try {
			comio.initScheduler(cdbID);
			comio.startScheduler();
		}
		catch (SchedulerStillRuningException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			comio.stopDroids();
		}


		try {

			comio.join();
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		comio.stopDroids();


	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Options opts = new Options();

		opts.addOption("h", "help", false, "Show this help");
		//opts.addOption("c", "config", true, "Config File");
		opts.addOption(OptionBuilder.withLongOpt("threads")
						.withDescription("Number of Threads (Default: " + THREADS_DEFAULT + ")")
						.withType(Number.class)
						.hasArg()
						.withArgName("no. of threads")
						.create("t"));
		opts.addOption(OptionBuilder.withLongOpt("config")
						.withDescription("Config File for Scheduler initialization")
						.hasArg()
						.withArgName("config file")
						.create("c"));

		CommandLineParser parser = new PosixParser();
		boolean showHelp = false;
		try {
			CommandLine cmd = parser.parse(opts, args);

			if (cmd.hasOption("h")) {
				showHelp = true;
			}
			else {
				int threads = THREADS_DEFAULT;
				File config = null;
				if (cmd.hasOption("t")) {
					threads = ((Number) cmd.getParsedOptionValue("t")).intValue();
				}
				if (cmd.hasOption("c")) {
					config = new File(cmd.getOptionValue("c"));
				}
				if (cmd.getArgs().length == 1) {
					File cdb = new File(cmd.getArgs()[0]);
					if(!cdb.canRead()) {
						LOGGER.log(Level.SEVERE, "Could not find CDB file: {0}", cdb.getAbsolutePath());
						return;
					}
					if(config != null && !config.canRead()) {
						LOGGER.log(Level.SEVERE, "Could not find Config file: {0}", config.getAbsolutePath());
						return;
					}
					runCDBTest(cdb, config, threads);
				}
				else {
					showHelp = true;
				}
			}
		}
		catch (ParseException ex) {
			System.out.println(ex.getMessage());
			showHelp = true;
		}
		catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		if (showHelp) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("testdist [options] CDB", opts);
		}
	}
}
