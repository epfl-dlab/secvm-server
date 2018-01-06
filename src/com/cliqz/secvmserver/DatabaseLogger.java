package com.cliqz.secvmserver;

import java.sql.SQLException;

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
