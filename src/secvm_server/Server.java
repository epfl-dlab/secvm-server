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
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
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
	
	
	JsonParser jsonParser;
	
	// Should the server be stopped?
	private boolean stop = false;
	// Is the server currently currently loading the next configurations
	// from the db or updating the corresponding files?
	private boolean loadingOrUpdatingConfigurations = false;
	Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations;
	Map<ServerRequestId, TestWeightsConfiguration> testConfigurations;
	
	// TODO: close these in the end if not null
	private Connection dbConnection;
	private PreparedStatement participationPackageInsertStatement;
	private PreparedStatement trainPackageInsertStatement;
	private PreparedStatement testPackageInsertStatement;
	private PreparedStatement getTrainConfigurationsStatement;
	private PreparedStatement getTestConfigurationsStatement;
	private PreparedStatement weightsInsertStatement;
	private PreparedStatement weightsUpdateStatement;
	private PreparedStatement gradientUpdateStatement;
	private PreparedStatement trainEndTimeUpdateStatement;
	
	public Server() {
		jsonParser = new JsonParser();
		
		try {
			dbConnection = establishDbConnection(DB_USER, DB_PASSWORD, DB_SERVER, DB_NAME);
			
			// TODO: maybe make this a bit nicer with a loop and a Map or alike
			participationPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_PARTICIPATION_DB
					.createPreparedStatement(dbConnection);
			trainPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_TRAIN_DB
					.createPreparedStatement(dbConnection);
			testPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_TEST_DB
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
			
			gradientUpdateStatement = SqlQueries
					.UPDATE_GRADIENT
					.createPreparedStatement(dbConnection);
			
			trainEndTimeUpdateStatement = SqlQueries
					.UPDATE_TRAIN_END_TIME
					.createPreparedStatement(dbConnection);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void start() {
		try {
			// TODO: put everything into this try catch
			Thread packageListener = new Thread(new PackageListener(PORT, NUM_THREADS_PROCESSING_INCOMING_PACKAGES));
			packageListener.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		boolean blubb = true;
		while (blubb) {
			try {
				Socket s = new Socket("127.0.0.1", PORT);
				OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());
				osw.write("{\n" + 
						"  \"e\": [1, 1],\n" + 
						"  \"p\": \"jkolk\",\n" + 
						"  \"l\": 1,\n" + 
						"  \"s\": 0\n" + 
						"}");
				osw.flush();
//				System.out.println(s.isConnected());
				ArrayList<Integer> l = new ArrayList<>();
				for (int i = 0; i < 10_000_000; ++i) {
					l.add(i);
				}
				l.clear();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		while (!stop) {
			try {
				loadingOrUpdatingConfigurations = true;
				// ***** Always call them in this order since loadTestConfigurations depends on the changes
				// loadTrainConfigurations makes to the db. *****
				trainConfigurations = loadTrainConfigurations();
				testConfigurations = loadTestConfigurations();
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
				String currGradientNotNormalizedBase64 = allTrainConfigurations.getString("weight_vector.gradient_not_normalized");
				AtomicIntegerArray currGradientNotNormalized = null;
				// can only be null if we are at the initial weight vector, i.e. iteration == 0
				if (currGradientNotNormalizedBase64 != null) {
					currGradientNotNormalized = DataUtils.base64ToAtomicIntegerArray(
							currGradientNotNormalizedBase64);
				}
				
				// TODO: Extend the testConfigurations SQL query and TestWeightsConfiguration to include all the data necessary for updates.
				
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
					// we need to apply the subgradient update to the weight vector
					// which isn't necessary in the case of the initial weight vector
					} else {
						DataUtils.applySubgradientUpdate(currWeights, iteration, lambda, currGradientNotNormalized, numParticipants);
						currWeightsBase64 = DataUtils.numberListToBase64(currWeights);
					}
					
					currGradientNotNormalized = new AtomicIntegerArray(numBins);
					currGradientNotNormalizedBase64 = DataUtils.atomicIntegerArrayToBase64(currGradientNotNormalized);
					
					
					// *** update the last iteration ***
					weightsUpdateStatement.setString(1, currWeightsBase64);
					weightsUpdateStatement.setInt(2, svmId);
					weightsUpdateStatement.setInt(3, iteration);
					weightsUpdateStatement.executeUpdate();
					
					// we don't need the old subgradient anymore
					gradientUpdateStatement.setString(1, null);
					gradientUpdateStatement.setInt(2, svmId);
					gradientUpdateStatement.setInt(3, iteration);
					gradientUpdateStatement.executeUpdate();
					
					trainEndTimeUpdateStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
					trainEndTimeUpdateStatement.setInt(2, svmId);
					trainEndTimeUpdateStatement.setInt(3, iteration);
					trainEndTimeUpdateStatement.executeUpdate();
					// *********************************
					
					++iteration;
					
					weightsInsertStatement.setInt(1, latestConfiguration.getSvmId());
					weightsInsertStatement.setInt(2, iteration);
					weightsInsertStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
					weightsInsertStatement.setInt(4, 0);
					weightsInsertStatement.setString(5, currWeightsBase64);
					weightsInsertStatement.setString(6, currGradientNotNormalizedBase64);
					weightsInsertStatement.executeUpdate();
				}
				
				latestConfiguration.setIteration(iteration);
				latestConfiguration.setGradientNotNormalized(currGradientNotNormalized);
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
			    try (BufferedReader socketReader = new BufferedReader(
			    		new InputStreamReader(socket.getInputStream()))) {
			    	JsonObject dataReceived = jsonParser.parse(socketReader).getAsJsonObject();			 
			    	UserPackage packageReceived = null;
			    	
			    	JsonArray requestIdArray = dataReceived.getAsJsonArray("e");
			    	int svmId = requestIdArray.get(0).getAsInt();
			    	int iteration = requestIdArray.get(1).getAsInt();
			    	
			    	String packageRandomId = dataReceived.get("p").getAsString();
			    	JsonElement predictedGenderJsonElement = dataReceived.get("s");
			    	// test package
			    	if (predictedGenderJsonElement != null) {
		    			int trueGender = dataReceived.get("l").getAsInt();
		    			int predictedGender = predictedGenderJsonElement.getAsInt();
		    			packageReceived = new TestPackage(
		    					svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()),
		    					trueGender, predictedGender);
		    			packageReceived.setAssociatedDbStatement(testPackageInsertStatement);
			    		// TODO: update testConfigurations
			    	} else {
			    		JsonElement updateValueJsonElement = dataReceived.get("v");
			    		// train package
			    		if (updateValueJsonElement != null) {
			    			int index = dataReceived.get("i").getAsInt();
			    			int value = updateValueJsonElement.getAsInt();
			    			packageReceived = new TrainPackage(
			    					svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()),
			    					index, value);
			    			packageReceived.setAssociatedDbStatement(trainPackageInsertStatement);
				    		// TODO: update trainConfigurations
			    		// participation package
			    		} else {
			    			packageReceived = new ParticipationPackage(
			    					svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()));
			    			packageReceived.setAssociatedDbStatement(participationPackageInsertStatement);
			    			// TODO: update trainConfigurations
			    		}
			    	}
			    	
			    	packageLoggingExecutor.submit(new DatabaseLogger(packageReceived));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

}
