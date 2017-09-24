package secvm_server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class Server {
	
	public static final String DB_USER = "java";
	public static final String DB_PASSWORD = "java";
	public static final String DB_SERVER = "localhost";
	public static final String DB_NAME = "SecVM_DB";
	
	// TODO: count experiment_id up in Base64 characters (to make it small)
	
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	
	// Should the server be stopped?
	private boolean stop = false;
	private ExecutorService packageLoggingExecutor;
	// TODO: close these in the end if not null
	private Connection dbConnection;
	private PreparedStatement participationPackageInsertStatement;
	private PreparedStatement trainPackageInsertStatement;
	private PreparedStatement testPackageInsertStatement;
	private PreparedStatement getTrainConfigurationStatement;
	
	public Server() {
		this.packageLoggingExecutor = Executors.newSingleThreadExecutor();
		try {
			dbConnection = establishDbConnection(DB_USER, DB_PASSWORD, DB_SERVER, DB_NAME);
			
			participationPackageInsertStatement = SqlQueries
					.INSERT_INTO_PARTICIPATION_DB
					.createPreparedStatement(dbConnection);
			// TODO: same for trainPackageInsertStatement, testPackageInsertStatement
			getTrainConfigurationStatement = SqlQueries
					.GET_TRAIN_CONFIGURATIONS
					.createPreparedStatement(dbConnection);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void start() {
		while (!stop) {
			try {
				Set<TrainWeightsConfiguration> trainConfigurations = new HashSet<>();
				// includes past iterations that are not interesting to us anymore
				ResultSet allTrainConfigurations = getTrainConfigurationStatement.executeQuery();
				// to see if we have arrived at a new svm
				int lastSvmId = -1;
				// if the current svm has already been added to trainConfigurations, its (smaller)
				// iterations can be skipped
				boolean currSvmAlreadyAdded = false;
				while (allTrainConfigurations.next()) {
					int currSvmId = allTrainConfigurations.getInt("svm.svm_id");
					if (currSvmId == lastSvmId && currSvmAlreadyAdded) {
						continue;
					}
					
					// the minimum participant quota for the last weight vector has been reached;
					// create a new one
					if (allTrainConfigurations.getInt("svm.min_number_train_participants")
							<= allTrainConfigurations.getInt("weight_vector.num_participants")) {
						
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		packageLoggingExecutor.shutdown();
		// wait until the logging is finished
		while (true) {
			try {
				if (packageLoggingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// only then close the db connection
		try {
			participationPackageInsertStatement.close();
			// TODO: same for trainPackageInsertStatement, testPackageInsertStatement
			dbConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		stop = true;
	}
	
	private Connection establishDbConnection(
			String user, String password, String serverName, String dbName)
					throws SQLException {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser(user);
		dataSource.setPassword(password);
		dataSource.setServerName(serverName);
		dataSource.setDatabaseName(dbName);
		return dataSource.getConnection();
	}

}
