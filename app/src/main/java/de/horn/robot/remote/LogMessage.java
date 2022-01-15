package de.horn.robot.remote;

public class LogMessage {

    String message;
    long timestamp;

    public LogMessage(String message) {
        this.message = message;
        timestamp = System.currentTimeMillis();
    }
}
