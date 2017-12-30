package com.cliqz.secvmserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

public class DatabaseLogger implements Runnable {
	private final UserPackage packageToLog;
	
	public DatabaseLogger(UserPackage packageToLog) {
        this.packageToLog = packageToLog;
	}

	@Override
	public void run() {
		try {
			packageToLog.fillStatement().executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
