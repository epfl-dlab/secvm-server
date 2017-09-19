package secvm_server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

public class DatabaseWriter implements Runnable {
	private final PreparedStatement statement;
	
	public DatabaseWriter(PreparedStatement statement) {
        this.statement = statement;
	}

	@Override
	public void run() {
		try {
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
