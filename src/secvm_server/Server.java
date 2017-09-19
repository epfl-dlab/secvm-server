package secvm_server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	
	public static void main(String[] args) {
		start();
	}
	
	private static void start() {
		Server server = new Server("data/user_data.db");
		server.onIncomingMessage();
	}
	
	
	private ExecutorService dbExecutor;
	// TODO: close these in the end if not null
	private Connection dbConnection;
	private PreparedStatement participationInsertStatement;
	
	private Server(String dbPath) {
		try {
			dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			
			new DatabaseWriter(SqlQueries
					.CREATE_PARTICIPATION_DB
					.createPreparedStatement(dbConnection)).run();
			
			participationInsertStatement = SqlQueries
					.INSERT_INTO_PARTICIPATION_DB
					.createPreparedStatement(dbConnection);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.dbExecutor = Executors.newSingleThreadExecutor();
	}
	
	private void onIncomingMessage() {
		try {
			dbExecutor.execute(
					new DatabaseWriter(
							(new ParticipationPackage("test_experimentId", "test_packageId", "test_features"))
								.fillStatement(participationInsertStatement)));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
