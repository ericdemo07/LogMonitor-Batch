# Log Monitoring Service


## What is it:
 
 * Log monitoring batch application will continuously monitor log files for any warning and will send a mail for the same.
 
 
## How to use it:

 * Mention path of required log file to be monitored in 'application.yml', and maill address of the group to be notified.
 * Because of its generic nature it can be bonded to any projects implementing logger api.
 * To stop this batch use the Pid provided in the batch logs and pass it as argument and start the batch again.
	 
## Basic useful feature list:

 * Can keep track of logs in complete directory
 * Resume function for reading logs(will resume where it left)
 * Uses lazy loading Stream reader of Java 8.
 