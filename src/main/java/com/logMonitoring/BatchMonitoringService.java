package com.logMonitoring;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*Watcher service to track change of directories
 * 
 */
public class BatchMonitoringService {
	final static Logger log = LoggerFactory.getLogger(BatchMonitoringService.class);
	private static WatchService watcher = null;
	private static Path path = null;
	private static long linePointer = 0L;
	private static BatchMonitoringService instance = null;
	private String statusFlag = "SUCCESS";
	private Map<String, Object> yamlData = new HashMap<>();

	private BatchMonitoringService() {
	}

	public static BatchMonitoringService getInstance() {
		if (instance == null) {
			instance = new BatchMonitoringService();
		}
		return instance;
	}

	public void sendMail(String subject, String body) throws MessagingException {
		Properties properties = System.getProperties();
		properties.setProperty(yamlData.get("mail.port").toString(), yamlData.get("mail.host").toString());
		Session session = Session.getDefaultInstance(properties);
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress((String) yamlData.get("mail.from")));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress((String) yamlData.get("mail.to")));
		message.setSubject(subject);
		message.setText(body);
		Transport.send(message);
		log.info("message sent");
	}

	void initializePathAndWatcher(String directoryPath, String directoryName, Map<String, Object> data)
			throws IOException {
		yamlData = data;
		Sigar sigar = new Sigar();
		long pid = sigar.getPid();
		log.debug("Pid for current process: [" + pid + "]");
		path = Paths.get(directoryPath, directoryName);
		log.debug("Directory being watched: [" + path.toAbsolutePath() + "]");
		watcher = FileSystems.getDefault().newWatchService();
		path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
		sigar.close();
	}

	void watcherServiceShutDown() throws IOException {
		watcher.close();
	}

	void trackEvent() throws Exception {
		try {
			while (true) {
				WatchKey watchKey;
				watchKey = watcher.take();
				for (WatchEvent<?> event : watchKey.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					@SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path fileName = ev.context();
					if (fileName.toString().equals(yamlData.get("directory.filename").toString())) {
						log.debug(kind.name() + ": [" + fileName + "]");
						StringBuilder absoluteFilePath = new StringBuilder(path.toString());
						absoluteFilePath.append("/");
						absoluteFilePath.append(fileName);
						Path filePath = Paths.get(absoluteFilePath.toString());
						long lineCount = Files.lines(filePath).count();
						Boolean flagMatchFound = false;
						if (statusFlag.equalsIgnoreCase("WARN")) {
							flagMatchFound = Files.lines(filePath).skip(linePointer).filter(s -> !s.isEmpty())
									.anyMatch(s -> s.contains(yamlData.get("success.keyword").toString()));
							if (flagMatchFound.equals(true)) {
								log.warn("Bing server is back to normal");
								statusFlag = "SUCCESS";
								sendMail((String) yamlData.get("mail.success.subject"),
										(String) yamlData.get("mail.success.body"));
							}
						}
						if (statusFlag.equalsIgnoreCase("SUCCESS")) {
							flagMatchFound = Files.lines(filePath).skip(linePointer).filter(s -> !s.isEmpty())
									.anyMatch(s -> s.contains(yamlData.get("error.keyword").toString()));
							if (flagMatchFound.equals(true)) {
								log.warn("Problem diagnosed on bing server");
								statusFlag = "WARN";
								sendMail((String) yamlData.get("mail.warn.subject"),
										(String) yamlData.get("mail.warn.body"));
							}
						}
						linePointer = lineCount;
						log.debug("Count of lines already processed active file: [" + linePointer + "]");
					}
				}
				watchKey.reset();
			}
		}
		catch(Exception e)
		{
			throw new Exception("Error occoured in trackEvent", e);
		}
		finally {
			watcher.close();
		}
	}
}
