package com.logMonitoring;

import java.io.FileReader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class InitiateMonitoring {
	final static Logger log = LoggerFactory.getLogger(InitiateMonitoring.class);
	private static final String APP_CONFIG_BASE = "src/main/resources";
	private static final String APP_CONFIG_NAME = "application.yml";
	static String appConfigPath = APP_CONFIG_BASE + "/" + APP_CONFIG_NAME;
	static Yaml yaml = new Yaml();

	public static void main(String... args) {
		try {
			if (args.length == 1) {
				StringBuilder taskKillerScript = new StringBuilder("kill -1 ");
				taskKillerScript.append(args[0]);
				Runtime.getRuntime().exec(taskKillerScript.toString());
				log.info("Task killed for PID: [" + args[0]);
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) yaml.load(new FileReader(appConfigPath));
				log.error("Started SMTP");
				log.debug("Started SMTP");
				BatchMonitoringService batchMonitoringService = BatchMonitoringService.getInstance();
				log.info("Thread : " + Thread.currentThread().getName());
				String directoryPath = (String) data.get("directory.path");
				String directoryName = (String) data.get("directory.name");
				log.info("\n[LogMonitoring-Batch:::::Start]");
				batchMonitoringService.initializePathAndWatcher(directoryPath, directoryName, data);
				batchMonitoringService.trackEvent();
			}
		} catch (Throwable e) {
			log.debug("Started SMTP");
			log.info("[LogMonitoringBatch Stopped due to following exception]");
			log.error(e.toString());
		} finally {
			log.info("[LogMonitoring-Batch:::::Stopped]\n");
		}
	}
}
