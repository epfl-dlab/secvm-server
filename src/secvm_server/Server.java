package secvm_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
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
	
	public static final int PORT = 8080;
	
	public static final int NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING = 2;
	
	public static final int NUM_THREADS_PROCESSING_INCOMING_PACKAGES = 8;
	
	// TODO: count experiment_id up in Base64 characters (to make it small)
	
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	
	// Should the server be stopped?
	private boolean stop = false;
	// Is the server currently currently loading the next configurations
	// from the db or updating the corresponding files?
	private boolean loadingOrUpdatingConfigurations = false;
	// TODO: close these in the end if not null
	private Connection dbConnection;
	private PreparedStatement participationPackageInsertStatement;
	private PreparedStatement trainPackageInsertStatement;
	private PreparedStatement testPackageInsertStatement;
	private PreparedStatement getTrainConfigurationsStatement;
	private PreparedStatement getTestConfigurationsStatement;
	private PreparedStatement weightsInsertStatement;
	private PreparedStatement weightsUpdateStatement;
	
	public Server() {
		try {
			dbConnection = establishDbConnection(DB_USER, DB_PASSWORD, DB_SERVER, DB_NAME);
			
			participationPackageInsertStatement = SqlQueries
					.INSERT_INTO_PARTICIPATION_DB
					.createPreparedStatement(dbConnection);
			// TODO: same for trainPackageInsertStatement, testPackageInsertStatement
			getTrainConfigurationsStatement = SqlQueries
					.GET_TRAIN_CONFIGURATIONS
					.createPreparedStatement(dbConnection);
			getTestConfigurationsStatement = SqlQueries
					.GET_TEST_CONFIGURATIONS
					.createPreparedStatement(dbConnection);
			
			weightsInsertStatement = SqlQueries
					.INSERT_INTO_WEIGHTS_DB
					.createPreparedStatement(dbConnection);
			
			weightsUpdateStatement = SqlQueries
					.UPDATE_WEIGHTS
					.createPreparedStatement(dbConnection);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void start() {
//		try {
//			// TODO: put everything into this try catch
//			Thread packageListener = new Thread(new PackageListener(PORT, NUM_THREADS_PROCESSING_INCOMING_PACKAGES));
//			packageListener.start();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		boolean blubb = true;
//		while (blubb) {
//			try {
//				Socket s = new Socket("127.0.0.1", PORT);
//				OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());
//				osw.write("this is a test string");
//				osw.flush();
////				System.out.println(s.isConnected());
//				ArrayList<Integer> l = new ArrayList<>();
//				for (int i = 0; i < 10_000_000; ++i) {
//					l.add(i);
//				}
//				l.clear();
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//		}
		
		while (!stop) {
			try {
				loadingOrUpdatingConfigurations = true;
				// ***** Always call them in this order since loadTestConfigurations depends on the changes
				// loadTrainConfigurations makes to the db. *****
				Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations = loadTrainConfigurations();
				Map<ServerRequestId, TestWeightsConfiguration> testConfigurations = loadTestConfigurations();
				// TODO: Override configuration file and weight files.
				loadingOrUpdatingConfigurations = false;
				// TODO: remove this
				stop = true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
//		packageLoggingExecutor.shutdown();
//		// wait until the logging is finished
//		while (true) {
//			try {
//				if (packageLoggingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
//					break;
//				}
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
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
	
	private Map<ServerRequestId, TestWeightsConfiguration> loadTestConfigurations() throws SQLException {
		Map<ServerRequestId, TestWeightsConfiguration> testConfigurations = new HashMap<>();
		// includes past iterations that are not interesting to us anymore
		ResultSet allTestConfigurations = getTestConfigurationsStatement.executeQuery();

		// the last configuration that was added to testConfigurations
		TestWeightsConfiguration latestConfiguration = new TestWeightsConfiguration();
		latestConfiguration.setSvmId(-1);
		latestConfiguration.setIteration(-1);
		
		while (allTestConfigurations.next()) {
			int svmId = allTestConfigurations.getInt("svm.svm_id");
			int iteration = allTestConfigurations.getInt("weight_vector.iteration");
			WeightsConfiguration.FeatureVectorProperties features = new WeightsConfiguration.FeatureVectorProperties(
					allTestConfigurations.getString("feature_vector.feature_type"),
					allTestConfigurations.getInt("feature_vector.num_features"),
					allTestConfigurations.getInt("feature_vector.num_hashes"));
			
			if (svmId == latestConfiguration.getSvmId()) {
				// we have a duplicate which means another new type of feature is added
				if (iteration == latestConfiguration.getIteration()) {
					latestConfiguration.addFeatures(features);
				// we have already summed up enough weight vectors
				} else if (latestConfiguration.getIteration() - iteration >= NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING){
					continue;
				// the weight vector needs to be taken into the average
				} else {
					List<Float> currWeights = DataUtils.base64ToNumberList(
							allTestConfigurations.getString("weight_vector.weights"));
					List<Float> summedWeights = latestConfiguration.getWeightsToUseForTesting();
					DataUtils.addToFirstVector(summedWeights, currWeights);
					
					// the necessary number of weight vectors has been summed up, now we need to normalize
					if (latestConfiguration.getIteration() - iteration + 1 == NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING) {
						DataUtils.divideVector(summedWeights, NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING);
					}
				}
			// the first time we encounter this svm instance
			} else {
				latestConfiguration = new TestWeightsConfiguration();
				
				List<Float> diceRollProbabilities = DataUtils.base64ToNumberList(
						allTestConfigurations.getString("dice_roll.probabilities"));
				List<Integer> testOutcomes = DataUtils.base64ToNumberList(
						allTestConfigurations.getString("svm.test_outcomes_dice_roll"));
				List<Float> currWeights = DataUtils.base64ToNumberList(
						allTestConfigurations.getString("weight_vector.weights"));
				
				latestConfiguration.setSvmId(svmId);
				latestConfiguration.setDiceRollProbabilities(diceRollProbabilities);
				latestConfiguration.setTestOutcomesDiceRoll(testOutcomes);
				latestConfiguration.addFeatures(features);
				
				latestConfiguration.setIteration(iteration);
				latestConfiguration.setWeightsToUseForTesting(currWeights);
				
				testConfigurations.put(
						new ServerRequestId(latestConfiguration.getSvmId(), latestConfiguration.getIteration()),
						latestConfiguration);
			}
		}
		
		return testConfigurations;
	}
	
	private Map<ServerRequestId, TrainWeightsConfiguration> loadTrainConfigurations() throws SQLException {
		Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations = new HashMap<>();
		// includes past iterations that are not interesting to us anymore
		ResultSet allTrainConfigurations = getTrainConfigurationsStatement.executeQuery();

		// the last configuration that was added to trainConfigurations
		TrainWeightsConfiguration latestConfiguration = new TrainWeightsConfiguration();
		latestConfiguration.setSvmId(-1);
		latestConfiguration.setIteration(-1);
		
		while (allTrainConfigurations.next()) {
			int svmId = allTrainConfigurations.getInt("svm.svm_id");
			int iteration = allTrainConfigurations.getInt("weight_vector.iteration");
			WeightsConfiguration.FeatureVectorProperties features = new WeightsConfiguration.FeatureVectorProperties(
					allTrainConfigurations.getString("feature_vector.feature_type"),
					allTrainConfigurations.getInt("feature_vector.num_features"),
					allTrainConfigurations.getInt("feature_vector.num_hashes"));
			
			if (svmId == latestConfiguration.getSvmId()) {
				if (iteration == latestConfiguration.getIteration()) {
					latestConfiguration.addFeatures(features);
				// this is just a previous iteration of the already added weights configuration
				} else {
					continue;
				}
			} else {
				latestConfiguration = new TrainWeightsConfiguration();
				
				float lambda = allTrainConfigurations.getFloat("svm.lambda");
				List<Float> diceRollProbabilities = DataUtils.base64ToNumberList(
						allTrainConfigurations.getString("dice_roll.probabilities"));
				List<Integer> trainOutcomes = DataUtils.base64ToNumberList(
						allTrainConfigurations.getString("svm.train_outcomes_dice_roll"));
				int numBins = allTrainConfigurations.getInt("svm.num_bins");
				String currWeightsBase64 = allTrainConfigurations.getString("weight_vector.weights");
				List<Float> currWeights = DataUtils.base64ToNumberList(currWeightsBase64);
				
				int minNumberTrainParticipants = allTrainConfigurations.getInt("svm.min_number_train_participants");
				int numParticipants = allTrainConfigurations.getInt("weight_vector.num_participants");
				
				latestConfiguration.setSvmId(svmId);
				latestConfiguration.setLambda(lambda);
				latestConfiguration.setNumBins(numBins);
				latestConfiguration.setDiceRollProbabilities(diceRollProbabilities);
				latestConfiguration.setTrainOutcomesDiceRoll(trainOutcomes);
				latestConfiguration.addFeatures(features);
				latestConfiguration.setMinNumberTrainParticipants(minNumberTrainParticipants);
			
				// the minimum participant quota for the last weight vector has been reached
				// or we are at the initial weight vector; create a new one
				if (numParticipants >= minNumberTrainParticipants || iteration == 0) {
					if (iteration == 0) {
						// wrapped in constructor to make it mutable
						currWeights = new ArrayList<>(Collections.nCopies(numBins, new Float(0)));
						currWeightsBase64 = DataUtils.numberListToBase64(currWeights);
						weightsUpdateStatement.setString(1, currWeightsBase64);
						weightsUpdateStatement.setInt(2, svmId);
						weightsUpdateStatement.setInt(3, iteration);
						weightsUpdateStatement.executeUpdate();
					}
					
					++iteration;
					// TODO: add training_end_time to previous weight_vector entry
					weightsInsertStatement.setInt(1, latestConfiguration.getSvmId());
					weightsInsertStatement.setInt(2, iteration);
					weightsInsertStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
					weightsInsertStatement.setInt(4, 0);
					weightsInsertStatement.setString(5, currWeightsBase64);
					weightsInsertStatement.executeUpdate();
				}
				
				latestConfiguration.setIteration(iteration);
				latestConfiguration.setGradientNotNormalized(new AtomicReferenceArray<>(numBins));
				latestConfiguration.setWeightsToUseForTraining(currWeights);
				
				trainConfigurations.put(
						new ServerRequestId(latestConfiguration.getSvmId(), latestConfiguration.getIteration()),
						latestConfiguration);
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
	
	class PackageListener implements Runnable {
		
		private ServerSocket serverSocket;
		private ExecutorService incomingPackageExecutor;
		private ExecutorService packageLoggingExecutor;

		public PackageListener(int port, int numThreadsPackageProcessing) throws IOException {
			this.serverSocket = new ServerSocket(port);
			this.incomingPackageExecutor = Executors.newFixedThreadPool(numThreadsPackageProcessing);
			this.packageLoggingExecutor = Executors.newSingleThreadExecutor();
		}

		@Override
		public void run() {
			while (!stop) {
				try {
					// TODO: add SecurityManager to check if the connection comes from the Cliqz proxy
					// and otherwise log this event in 'catch (SecurityException e)' 
					Socket socket = serverSocket.accept();
					incomingPackageExecutor.submit(new PackageHandler(socket));
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			// TODO: Do something like this:
//			packageLoggingExecutor.shutdown();
//			// wait until the logging is finished
//			while (true) {
//				try {
//					if (packageLoggingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
//						break;
//					}
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
		}
		
		class PackageHandler implements Runnable {
			
			private Socket socket;
			
			public PackageHandler(Socket socket) {
				this.socket = socket;
			}

			@Override
			public void run() {
			    try (BufferedReader socketIn = new BufferedReader(
			    		new InputStreamReader(socket.getInputStream()))) {
			    	System.out.println(socketIn.readLine());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

}
