package secvm_server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
				Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations = loadTrainConfigurations();
				// TODO: remove this
				stop = true;
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
	
	private Map<ServerRequestId, TrainWeightsConfiguration> loadTrainConfigurations() throws SQLException {
		Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations = new HashMap<>();
		// includes past iterations that are not interesting to us anymore
		ResultSet allTrainConfigurations = getTrainConfigurationStatement.executeQuery();

		// the last configuration that was added to trainConfigurations
		TrainWeightsConfiguration lastConfiguration = new TrainWeightsConfiguration();
		lastConfiguration.setSvmId(-1);
		lastConfiguration.setIteration(-1);
		
		while (allTrainConfigurations.next()) {
			int svmId = allTrainConfigurations.getInt("svm.svm_id");
			int iteration = allTrainConfigurations.getInt("weight_vector.iteration");
			WeightsConfiguration.FeatureVectorProperties features = new WeightsConfiguration.FeatureVectorProperties(
					allTrainConfigurations.getString("feature_vector.feature_type"),
					allTrainConfigurations.getInt("feature_vector.num_features"),
					allTrainConfigurations.getInt("feature_vector.num_hashes"));
			
			if (svmId == lastConfiguration.getSvmId()) {
				if (iteration == lastConfiguration.getIteration()) {
					lastConfiguration.addFeatures(features);
				// this is just a previous iteration of the already added weights configuration
				} else {
					continue;
				}
			} else {
				lastConfiguration = new TrainWeightsConfiguration();
				
				float lambda = allTrainConfigurations.getFloat("svm.lambda");
				List<Float> diceRollProbabilities = DataConverter.base64ToNumberList(
						allTrainConfigurations.getString("dice_roll.probabilities"));
				List<Integer> trainOutcomes = DataConverter.base64ToNumberList(
						allTrainConfigurations.getString("svm.train_outcomes_dice_roll"));
				int numBins = allTrainConfigurations.getInt("svm.num_bins");
				List<Float> currWeights = DataConverter.base64ToNumberList(
						allTrainConfigurations.getString("weight_vector.weights"));
				
				int minNumberTrainParticipants = allTrainConfigurations.getInt("svm.min_number_train_participants");
				int numParticipants = allTrainConfigurations.getInt("weight_vector.num_participants");
				
				lastConfiguration.setSvmId(svmId);
				lastConfiguration.setLambda(lambda);
				lastConfiguration.setNumBins(numBins);
				lastConfiguration.setDiceRollProbabilities(diceRollProbabilities);
				lastConfiguration.setTrainOutcomesDiceRoll(trainOutcomes);
				lastConfiguration.addFeatures(features);
				lastConfiguration.setMinNumberTrainParticipants(minNumberTrainParticipants);
			
				// TODO: check how the insertion/modification of the db affects in which order
				// we should fetch the train and the test configurations from the db
				
				// the minimum participant quota for the last weight vector has been reached
				// or we are at the initial weight vector; create a new one
				if (numParticipants >= minNumberTrainParticipants || iteration == 0) {
					// TODO: add training_end_time to previous weight_vector entry
					// TODO: create new entry in weight_vector table
					if (iteration == 0) {
						// wrapped in constructor to make it mutable
						currWeights = new ArrayList<>(Collections.nCopies(numBins, new Float(0)));
						// TODO: update cell "weights" of previous weight_vector entry with the zero vector
						// of length numBins, i.e. currWeights
					}
				}
				
				lastConfiguration.setIteration(iteration + 1);
				lastConfiguration.setGradientNotNormalized(new AtomicReferenceArray<>(numBins));
				lastConfiguration.setWeightsToUseForTraining(currWeights);
				
				trainConfigurations.put(
						new ServerRequestId(lastConfiguration.getSvmId(), lastConfiguration.getIteration()),
						lastConfiguration);
			}
		}
		
		return trainConfigurations;
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
