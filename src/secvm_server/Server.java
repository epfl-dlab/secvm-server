package secvm_server;

import java.beans.FeatureDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import secvm_server.WeightsConfiguration.FeatureVectorProperties;
import secvm_server.json_objects.ExperimentConfigurationForClients;
import secvm_server.json_objects.WeightsForClients;

public class Server implements Runnable {
	
	public static final String DB_USER = "java";
	public static final String DB_PASSWORD = "java";
	public static final String DB_SERVER = "localhost";
	public static final String DB_NAME = "SecVM_DB";
	
//	public static final String CONFIGURATION_FILE_PATH = "configuration.json";
//	public static final String WEIGHTS_FILE_NETWORK_PATH = "http://localhost:8000/";
//	public static final String WEIGHTS_FILE_LOCAL_PATH = "./";

	public static final String CONFIGURATION_FILE_PATH = "../experiment-configs/configuration.json";
	public static final String WEIGHTS_FILE_NETWORK_PATH = "https://svm.cliqz.com/";
	public static final String WEIGHTS_FILE_LOCAL_PATH = "../experiment-configs/";
	
	public static final int PORT = 8081;
	
	public static final String PACKAGE_RESPONSE;
	
	static {
		JsonObject packageObject = new JsonObject();
		packageObject.addProperty("result", true);
		PACKAGE_RESPONSE = packageObject.toString();
	}
	
	public static final int NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING = 2;
	
	public static final int NUM_THREADS_PROCESSING_INCOMING_PACKAGES = 8;
	
	// TODO: maybe make those parameter of main()
	public static final long MILLIS_TO_WAIT_FOR_RECEIVING_USER_PACKAGES = 1000;
	public static final long MILLIS_TO_WAIT_AFTER_END_OF_DEADLINE = 1000;
	public static final int SECONDS_TO_WAIT_FOR_HTTP_SERVER_TO_STOP = 100;
	
	public static void main(String[] args) {
		
		Server server = new Server();
		Thread mainServerThread = new Thread(server);
		mainServerThread.start();
		
		try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
			String input = inputReader.readLine();
			while (!input.equals("stop")) {
				input = inputReader.readLine();
			}
			
			server.stop();
			try {
				mainServerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("stopped");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
	JsonParser jsonParser;
	
	// Should the server be stopped?
	private volatile boolean stop = false;
	// to not access the weights configurations while the server is fetching new ones
	// from the db
	// TODO: maybe remove the "true" parameter for better performance
	// TODO: for even better performance: omit the locks completely and just catch
	// NullPointerExceptions in the PackageHandler
	private final ReadWriteLock trainWeightsConfigurationsLock = new ReentrantReadWriteLock(true);
	private final ReadWriteLock testWeightsConfigurationsLock = new ReentrantReadWriteLock(true);
	
	Map<ServerRequestId, TrainWeightsConfiguration> trainConfigurations;
	Map<ServerRequestId, TestWeightsConfiguration> testConfigurations;
	
	private Connection dbConnection;
	private PreparedStatement participationPackageInsertStatement;
	private PreparedStatement trainPackageInsertStatement;
	private PreparedStatement testPackageInsertStatement;
	private PreparedStatement getTrainConfigurationsStatement;
	private PreparedStatement getTestConfigurationsStatement;
	private PreparedStatement getTestAccuracyStatement;
	private PreparedStatement testAccuracyInsertStatement;
	private PreparedStatement weightsInsertStatement;
	private PreparedStatement weightsUpdateStatement;
	private PreparedStatement gradientUpdateStatement;
	private PreparedStatement trainEndTimeUpdateStatement;
	private PreparedStatement gradientNumParticipantsUpdateStatement;
	private PreparedStatement testResultsUpdateStatement;
	private PreparedStatement getParticipationRandomIdsStatement;
	private PreparedStatement getTrainRandomIdsStatement;
	private PreparedStatement getTestRandomIdsStatement;
	
	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	
	
	public Server() {
		jsonParser = new JsonParser();
		
		try {
			dbConnection = establishDbConnection(DB_USER, DB_PASSWORD, DB_SERVER, DB_NAME);
			
			// TODO: maybe make this a bit nicer with a loop and a Map or alike
			participationPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_PARTICIPATION_TABLE
					.createPreparedStatement(dbConnection);
			trainPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_TRAIN_TABLE
					.createPreparedStatement(dbConnection);
			testPackageInsertStatement = SqlQueries
					.INSERT_INTO_PACKAGE_TEST_TABLE
					.createPreparedStatement(dbConnection);
			
			getTrainConfigurationsStatement = SqlQueries
					.GET_TRAIN_CONFIGURATIONS
					.createPreparedStatement(dbConnection);
			getTestConfigurationsStatement = SqlQueries
					.GET_TEST_CONFIGURATIONS
					.createPreparedStatement(dbConnection);
			
			getTestAccuracyStatement = SqlQueries
					.GET_TEST_ACCURACY
					.createPreparedStatement(dbConnection);
			testAccuracyInsertStatement = SqlQueries
					.INSERT_INTO_TEST_ACCURACY_TABLE
					.createPreparedStatement(dbConnection);
			
			weightsInsertStatement = SqlQueries
					.INSERT_INTO_WEIGHTS_TABLE
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
			
			gradientNumParticipantsUpdateStatement = SqlQueries
					.UPDATE_GRADIENT_NUM_PARTICIPANTS
					.createPreparedStatement(dbConnection);
			
			testResultsUpdateStatement = SqlQueries
					.UPDATE_TEST_RESULTS
					.createPreparedStatement(dbConnection);
			
			getParticipationRandomIdsStatement = SqlQueries
					.GET_PARTICIPATION_PACKAGE_RANDOM_IDS
					.createPreparedStatement(dbConnection);
			
			getTrainRandomIdsStatement = SqlQueries
					.GET_TRAIN_PACKAGE_RANDOM_IDS
					.createPreparedStatement(dbConnection);
			
			getTestRandomIdsStatement = SqlQueries
					.GET_TEST_PACKAGE_RANDOM_IDS
					.createPreparedStatement(dbConnection);
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public void run() {
		try {
			// TODO: maybe specify the maximum number of queued connections (second parameter)
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
			
			// for handling incoming packages
			ExecutorService packageHandlerExecutor = Executors.newFixedThreadPool(NUM_THREADS_PROCESSING_INCOMING_PACKAGES);
			// for writing incoming packages to the db
			ExecutorService packageLoggingExecutor = Executors.newSingleThreadExecutor();
			
			httpServer.createContext("/", new PackageHandler(packageLoggingExecutor));
			httpServer.setExecutor(null);
			httpServer.start();
			
			outer: while (true) {
				// ***** Always call train and test configuration loading in this order
				// since loadTestConfigurations depends on the changes loadTrainConfigurations
				// makes to the db. *****
				trainWeightsConfigurationsLock.writeLock().lock();
				try {
					trainConfigurations = loadTrainConfigurations();
				} finally {
					trainWeightsConfigurationsLock.writeLock().unlock();
				}
				testWeightsConfigurationsLock.writeLock().lock();
				try {
					testConfigurations = loadTestConfigurations();
				} finally {
					testWeightsConfigurationsLock.writeLock().unlock();
				}
				
				
				// write the configuration file and weight files for the clients
				JsonObject configurationJson = createConfigurationAndWeightFilesFromWeightsConfigurations(
						trainConfigurations.values(), testConfigurations.values());
				
				
				long deadline = System.currentTimeMillis() +
						MILLIS_TO_WAIT_FOR_RECEIVING_USER_PACKAGES;

				while (System.currentTimeMillis() < deadline) {
					if (stop) {
						break outer;
					}
					int timeLeft = (int) (deadline - System.currentTimeMillis());
					// update the time that is left for the clients to send their updates
					if (timeLeft > 0) {
						configurationJson.remove("timeLeft");
						JsonArray updatedTimesLeft = new JsonArray();
						Collections.nCopies(trainConfigurations.size() + testConfigurations.size(), timeLeft).
							forEach(updatedTimesLeft::add);
						configurationJson.add("timeLeft", updatedTimesLeft);
						JsonObject wrappedConfigurationJson = DataUtils.wrapJsonInResultObject(configurationJson);
						DataUtils.writeStringToFile(wrappedConfigurationJson.toString(), CONFIGURATION_FILE_PATH);
					}

					Thread.sleep(1000);
				}
				
				Thread.sleep(MILLIS_TO_WAIT_AFTER_END_OF_DEADLINE);
				
				
				// The entries are guaranteed to already be in the db because if they aren't
				// already at the beginning of the iteration, they will be created during
				// loadTrainConfigurations() andloadTestConfigurations(). 
				
				for (TrainWeightsConfiguration trainConfig : trainConfigurations.values()) {
					updateWeightVectorTableAfterTraining(trainConfig);
				}
				
				for (TestWeightsConfiguration testConfig : testConfigurations.values()) {
					updateTestAccuracyTableAfterTesting(testConfig);
				}
			}
			
			
			// shut down
			
			System.out.println("stopping ...");
			
			// Somehow this blocks until the end of the delay, even when the server hasn't
			// received any connections at all, so we just set a delay of 1 second.
			// But since we have an externally managed Executor we still don't lose any of
			// the packages coming from open connections because the threads are alive
			// until the ExecutorService is shut down.
//			httpServer.stop(SECONDS_TO_WAIT_FOR_HTTP_SERVER_TO_STOP);
			httpServer.stop(1);
			
			packageHandlerExecutor.shutdown();
			while (true) {
				try {
					if (packageHandlerExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			
			// packageLoggingExecutor can only be shut down after the termination of incomingPackageExecutor
			// because the latter uses the former one.
			packageLoggingExecutor.shutdown();
			while (true) {
				try {
					if (packageLoggingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		// continue shutting down
		
		// delete the configuration file so the clients don't fetch the old configuration
		try {
			Files.deleteIfExists(Paths.get(CONFIGURATION_FILE_PATH));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
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
		
		// the iteration number of the last weight vector we've seen
		int lastSeenIteration = -1;
		
		while (allTestConfigurations.next()) {
			int svmId = allTestConfigurations.getInt("svm.svm_id");
			int iteration = allTestConfigurations.getInt("weight_vector.iteration");
			WeightsConfiguration.FeatureVectorProperties features = new WeightsConfiguration.FeatureVectorProperties(
					allTestConfigurations.getString("feature_vector.feature_type"),
					allTestConfigurations.getInt("feature_vector.id"),
					allTestConfigurations.getInt("feature_vector.num_features"),
					allTestConfigurations.getInt("feature_vector.num_hashes"));
			
			if (svmId == latestConfiguration.getSvmId()) {
				// We have a duplicate which means another new type of feature is added.
				if (iteration == latestConfiguration.getIteration()) {
					latestConfiguration.addFeatures(features);
				} else if (iteration == lastSeenIteration) {
					continue;
				} else if (
						// We have already summed up enough weight vectors.
						latestConfiguration.getIteration() - iteration >= NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING ||
						// We have a duplicate as above but not of the weight vector with the highest iteration number.
						// This means that we have already added this type of feature (when we took the branch above)
						// and don't need to do anything else.
						iteration == lastSeenIteration) {
					continue;
				// the weight vector needs to be taken into the average
				} else {
					List<Float> currWeights = DataUtils.base64ToFloatList(
							allTestConfigurations.getString("weight_vector.weights"));
					List<Float> summedWeights = latestConfiguration.getWeightsToUseForTesting();
					DataUtils.addToFirstVector(summedWeights, currWeights);
					
					// the necessary number of weight vectors has been summed up, now we need to normalize
					if (latestConfiguration.getIteration() - iteration + 1 == NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING) {
						DataUtils.divideVector(summedWeights, NUM_WEIGHT_VECTORS_TO_AVERAGE_FOR_TESTING);
					}
					
					lastSeenIteration = iteration;
				}
			// the first time we encounter this svm instance
			} else {
				latestConfiguration = new TestWeightsConfiguration();
				
				List<Float> diceRollProbabilities = DataUtils.base64ToFloatList(
						allTestConfigurations.getString("dice_roll.probabilities"));
				List<Integer> testOutcomes = DataUtils.base64ToIntegerList(
						allTestConfigurations.getString("svm.test_outcomes_dice_roll"));
				List<Float> currWeights = DataUtils.base64ToFloatList(
						allTestConfigurations.getString("weight_vector.weights"));
				
				latestConfiguration.setSvmId(svmId);
				latestConfiguration.setDiceRollId(allTestConfigurations.getInt("dice_roll.id"));
				latestConfiguration.setDiceRollProbabilities(diceRollProbabilities);
				latestConfiguration.setTestOutcomesDiceRoll(testOutcomes);
				latestConfiguration.addFeatures(features);
				
				latestConfiguration.setIteration(iteration);
				latestConfiguration.setWeightsToUseForTesting(currWeights);
				
				
				getTestAccuracyStatement.setInt(1, svmId);
				getTestAccuracyStatement.setInt(2, iteration);
				ResultSet alreadyStoredTestResults = getTestAccuracyStatement.executeQuery();

				if (alreadyStoredTestResults.next()) {
					latestConfiguration.setFemaleOverall(alreadyStoredTestResults.getInt(1));
					latestConfiguration.setMaleOverall(alreadyStoredTestResults.getInt(2));
					latestConfiguration.setFemaleCorrect(alreadyStoredTestResults.getInt(3));
					latestConfiguration.setMaleCorrect(alreadyStoredTestResults.getInt(4));
				// The ResultSet is empty which means that there is no entry for this test configuration
				// in the db yet. Create a new one.
				} else {
					latestConfiguration.setFemaleOverall(0);
					latestConfiguration.setMaleOverall(0);
					latestConfiguration.setFemaleCorrect(0);
					latestConfiguration.setMaleCorrect(0);
					testAccuracyInsertStatement.setInt(1, svmId);
					testAccuracyInsertStatement.setInt(2, iteration);
					testAccuracyInsertStatement.setInt(3, 0);
					testAccuracyInsertStatement.setInt(4, 0);
					testAccuracyInsertStatement.setInt(5, 0);
					testAccuracyInsertStatement.setInt(6, 0);
					testAccuracyInsertStatement.executeUpdate();
				}
				
				alreadyStoredTestResults.close();
				
				getTestRandomIdsStatement.setInt(1, svmId);
				getTestRandomIdsStatement.setInt(2, iteration);
				ResultSet alreadyReceivedRandomIds = getTestRandomIdsStatement.executeQuery();
				while (alreadyReceivedRandomIds.next()) {
					latestConfiguration.addTestPackageRandomId(alreadyReceivedRandomIds.getString(1));
				}
				alreadyReceivedRandomIds.close();
				
				testConfigurations.put(
						new ServerRequestId(latestConfiguration.getSvmId(), latestConfiguration.getIteration()),
						latestConfiguration);
				
				lastSeenIteration = -1;
			}
		}
		
		allTestConfigurations.close();
		
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
		
		Timestamp timestampNow = new Timestamp(System.currentTimeMillis());
		
		while (allTrainConfigurations.next()) {
			int svmId = allTrainConfigurations.getInt("svm.svm_id");
			int iteration = allTrainConfigurations.getInt("weight_vector.iteration");
			WeightsConfiguration.FeatureVectorProperties features = new WeightsConfiguration.FeatureVectorProperties(
					allTrainConfigurations.getString("feature_vector.feature_type"),
					allTrainConfigurations.getInt("feature_vector.id"),
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
				List<Float> diceRollProbabilities = DataUtils.base64ToFloatList(
						allTrainConfigurations.getString("dice_roll.probabilities"));
				List<Integer> trainOutcomes = DataUtils.base64ToIntegerList(
						allTrainConfigurations.getString("svm.train_outcomes_dice_roll"));
				int numBins = allTrainConfigurations.getInt("svm.num_bins");
				String currWeightsBase64 = allTrainConfigurations.getString("weight_vector.weights");
				List<Float> currWeights = DataUtils.base64ToFloatList(currWeightsBase64);
				String currGradientNotNormalizedBase64 = allTrainConfigurations.getString("weight_vector.gradient_not_normalized");
				AtomicIntegerArray currGradientNotNormalized = null;
				// can only be null if we are at the initial weight vector, i.e. iteration == 0
				if (currGradientNotNormalizedBase64 != null) {
					currGradientNotNormalized = DataUtils.base64ToAtomicIntegerArray(
							currGradientNotNormalizedBase64);
				}
				
				int minNumberTrainParticipants = allTrainConfigurations.getInt("svm.min_number_train_participants");
				int numParticipants = allTrainConfigurations.getInt("weight_vector.num_participants");
				
				latestConfiguration.setSvmId(svmId);
				latestConfiguration.setLambda(lambda);
				latestConfiguration.setNumBins(numBins);
				latestConfiguration.setDiceRollId(allTrainConfigurations.getInt("dice_roll.id"));
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
						currWeightsBase64 = DataUtils.floatListToBase64(currWeights);
					// we need to apply the subgradient update to the weight vector
					// which isn't necessary in the case of the initial weight vector
					} else {
						DataUtils.applySubgradientUpdate(currWeights, iteration, lambda, currGradientNotNormalized, numParticipants);
						currWeightsBase64 = DataUtils.floatListToBase64(currWeights);
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
					
					trainEndTimeUpdateStatement.setTimestamp(1, timestampNow);
					trainEndTimeUpdateStatement.setInt(2, svmId);
					trainEndTimeUpdateStatement.setInt(3, iteration);
					trainEndTimeUpdateStatement.executeUpdate();
					// *********************************
					
					++iteration;
					
					weightsInsertStatement.setInt(1, latestConfiguration.getSvmId());
					weightsInsertStatement.setInt(2, iteration);
					weightsInsertStatement.setTimestamp(3, timestampNow);
					weightsInsertStatement.setInt(4, 0);
					weightsInsertStatement.setString(5, currWeightsBase64);
					weightsInsertStatement.setString(6, currGradientNotNormalizedBase64);
					weightsInsertStatement.executeUpdate();
				}
				
				latestConfiguration.setIteration(iteration);
				latestConfiguration.setGradientNotNormalized(currGradientNotNormalized);
				latestConfiguration.setWeightsToUseForTraining(currWeights);
				
				getParticipationRandomIdsStatement.setInt(1, svmId);
				getParticipationRandomIdsStatement.setInt(2, iteration);
				ResultSet alreadyReceivedParticipationRandomIds = getParticipationRandomIdsStatement.executeQuery();
				while (alreadyReceivedParticipationRandomIds.next()) {
					latestConfiguration.addParticipationPackageRandomId(
							alreadyReceivedParticipationRandomIds.getString(1));
				}
				alreadyReceivedParticipationRandomIds.close();
				
				getTrainRandomIdsStatement.setInt(1, svmId);
				getTrainRandomIdsStatement.setInt(2, iteration);
				ResultSet alreadyReceivedTrainRandomIds = getTrainRandomIdsStatement.executeQuery();
				while (alreadyReceivedTrainRandomIds.next()) {
					latestConfiguration.addTrainPackageRandomId(alreadyReceivedTrainRandomIds.getString(1));
				}
				alreadyReceivedTrainRandomIds.close();
				
				trainConfigurations.put(
						new ServerRequestId(latestConfiguration.getSvmId(), latestConfiguration.getIteration()),
						latestConfiguration);
			}
		}
		
		allTrainConfigurations.close();
		
		return trainConfigurations;
	}
	
	/**
	 * @return the configuration as JSON (as it is written to the configuration file, but not wrapped)
	 */
	private JsonObject createConfigurationAndWeightFilesFromWeightsConfigurations(
			Collection<TrainWeightsConfiguration> trainConfigurations,
			Collection<TestWeightsConfiguration> testConfigurations) {

		// to number the weight vector files
		int experimentIndex = 0;
		int numExperiments = trainConfigurations.size() + testConfigurations.size();
		ExperimentConfigurationForClients experimentConfiguration = new ExperimentConfigurationForClients();
		experimentConfiguration.experimentId = new ExperimentConfigurationForClients.ExperimentId[numExperiments];
		experimentConfiguration.features = new ExperimentConfigurationForClients.Features[numExperiments];
		experimentConfiguration.diceRolls = new ExperimentConfigurationForClients.DiceRolls[numExperiments];
		experimentConfiguration.weightVectorUrl = new String[numExperiments];
		experimentConfiguration.timeLeft = new int[numExperiments];
		
		for (TrainWeightsConfiguration trainConfig : trainConfigurations) {
			fillExperimentConfigurationEntry(trainConfig, experimentConfiguration, experimentIndex);
			
			experimentConfiguration.diceRolls[experimentIndex] =
					new ExperimentConfigurationForClients.DiceRolls(
							String.valueOf(trainConfig.getDiceRollId()),
							DataUtils.floatListToArray(trainConfig.getDiceRollProbabilities()),
							DataUtils.intListToArray(trainConfig.getTrainOutcomesDiceRoll()),
							null);
			
			WeightsForClients weightsForClients = new WeightsForClients(
					DataUtils.floatListToBase64(trainConfig.getWeightsToUseForTraining()));
			JsonObject wrappedWeightsJson = DataUtils.wrapJsonInResultObject(gson.toJsonTree(weightsForClients));
			DataUtils.writeStringToFile(
					wrappedWeightsJson.toString(),
					WEIGHTS_FILE_LOCAL_PATH + experimentConfiguration.weightVectorUrl[experimentIndex]);
			
			++experimentIndex;
		}
		
		for (TestWeightsConfiguration testConfig : testConfigurations) {
			fillExperimentConfigurationEntry(testConfig, experimentConfiguration, experimentIndex);
			
			experimentConfiguration.diceRolls[experimentIndex] =
					new ExperimentConfigurationForClients.DiceRolls(
							String.valueOf(testConfig.getDiceRollId()),
							DataUtils.floatListToArray(testConfig.getDiceRollProbabilities()),
							null,
							DataUtils.intListToArray(testConfig.getTestOutcomesDiceRoll()));

			WeightsForClients weightsForClients = new WeightsForClients(
					DataUtils.floatListToBase64(testConfig.getWeightsToUseForTesting()));
			JsonObject wrappedWeightsJson = DataUtils.wrapJsonInResultObject(gson.toJsonTree(weightsForClients));
			DataUtils.writeStringToFile(
					wrappedWeightsJson.toString(),
					WEIGHTS_FILE_LOCAL_PATH + experimentConfiguration.weightVectorUrl[experimentIndex]);
			
			++experimentIndex;
		}
		
		for (int i = 0; i < experimentConfiguration.weightVectorUrl.length; ++i) {
			experimentConfiguration.weightVectorUrl[i] =
					WEIGHTS_FILE_NETWORK_PATH + experimentConfiguration.weightVectorUrl[i];
		}
		
		JsonObject configurationJson = (JsonObject) gson.toJsonTree(experimentConfiguration);
		JsonObject wrappedConfigurationJson = DataUtils.wrapJsonInResultObject(configurationJson);
		DataUtils.writeStringToFile(wrappedConfigurationJson.toString(), CONFIGURATION_FILE_PATH);
		
		return configurationJson;
	}
	
	/*
	 * experimentConfiguration.weightVectorUrl only contains the names of the weight vector files,
	 * not the full paths.
	 */
	private void fillExperimentConfigurationEntry(
			WeightsConfiguration weightsConfiguration, ExperimentConfigurationForClients experimentConfiguration, int experimentIndex) {
		
		experimentConfiguration.experimentId[experimentIndex] =
				new ExperimentConfigurationForClients.ExperimentId(weightsConfiguration.getSvmId(), weightsConfiguration.getIteration());
		
		ExperimentConfigurationForClients.Features currFeaturesConfigEntry =
				new ExperimentConfigurationForClients.Features();
		List<FeatureVectorProperties> currFeatureProperties = weightsConfiguration.getFeatures();
		for (FeatureVectorProperties properties : currFeatureProperties) {
			if (properties.getFeature_type() == "hosts") {
				currFeaturesConfigEntry.idHosts = String.valueOf(properties.getId());
				currFeaturesConfigEntry.numHosts = properties.getNum_features();
				currFeaturesConfigEntry.numHashesHosts = properties.getNum_hashes();
			} else {
				currFeaturesConfigEntry.idTitleWords = String.valueOf(properties.getId());
				currFeaturesConfigEntry.numTitleWords = properties.getNum_features();
				currFeaturesConfigEntry.numHashesTitleWords = properties.getNum_hashes();
			}
		}
		experimentConfiguration.features[experimentIndex] = currFeaturesConfigEntry;
		
		experimentConfiguration.weightVectorUrl[experimentIndex] = "weights" + experimentIndex + ".json";
		
		experimentConfiguration.timeLeft[experimentIndex] = (int) MILLIS_TO_WAIT_FOR_RECEIVING_USER_PACKAGES;
	}
	
	private void updateWeightVectorTableAfterTraining (TrainWeightsConfiguration trainConfig) throws SQLException {
		gradientNumParticipantsUpdateStatement.setString(1,
				DataUtils.atomicIntegerArrayToBase64(
						trainConfig.getGradientNotNormalized()));
		gradientNumParticipantsUpdateStatement.setInt(2,
				trainConfig.getNumParticipants());
		gradientNumParticipantsUpdateStatement.setInt(3,
				trainConfig.getSvmId());
		gradientNumParticipantsUpdateStatement.setInt(4,
				trainConfig.getIteration());
		gradientNumParticipantsUpdateStatement.executeUpdate();
	}
	
	private void updateTestAccuracyTableAfterTesting (TestWeightsConfiguration testConfig) throws SQLException {
		testResultsUpdateStatement.setInt(1,
				testConfig.getFemaleOverall());
		testResultsUpdateStatement.setInt(2,
				testConfig.getMaleOverall());
		testResultsUpdateStatement.setInt(3,
				testConfig.getFemaleCorrect());
		testResultsUpdateStatement.setInt(4,
				testConfig.getMaleCorrect());
		testResultsUpdateStatement.setInt(5,
				testConfig.getSvmId());
		testResultsUpdateStatement.setInt(6,
				testConfig.getIteration());
		testResultsUpdateStatement.executeUpdate();
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

	
	class PackageHandler implements HttpHandler {
		
		private ExecutorService packageLoggingExecutor;
		
		public PackageHandler(ExecutorService packageLoggingExecutor) {
			this.packageLoggingExecutor = packageLoggingExecutor;
		}

		// If the received data isn't of one of the three package types expected,
		// then a RuntimeException (either NullPointerException or one of the Gson
		// Exceptions) will be thrown and the package will be discarded.
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			try (BufferedReader socketReader = new BufferedReader(
					new InputStreamReader(httpExchange.getRequestBody()))) {
				// if the data isn't valid JSON, this will throw a RuntimeException
				JsonElement jsonElementReceived = jsonParser.parse(socketReader);
				JsonObject objectReceived = jsonElementReceived.getAsJsonObject();
				UserPackage packageReceived = null;

				JsonArray requestIdArray = objectReceived.getAsJsonArray("e");
				int svmId = requestIdArray.get(0).getAsInt();
				int iteration = requestIdArray.get(1).getAsInt();

				String packageRandomId = objectReceived.get("p").getAsString();
				JsonElement predictedGenderJsonElement = objectReceived.get("s");
				// test package
				if (predictedGenderJsonElement != null) {
					int trueGender = objectReceived.get("l").getAsInt();
					int predictedGender = predictedGenderJsonElement.getAsInt();
					packageReceived = new TestPackage(
							svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()),
							trueGender, predictedGender);
					packageReceived.setAssociatedDbStatement(testPackageInsertStatement);

					testWeightsConfigurationsLock.readLock().lock();
					try {
						TestWeightsConfiguration configurationToUpdate =
								testConfigurations.get(new ServerRequestId(svmId, iteration));
						// <package not outdated> && <no duplicate>
						if (configurationToUpdate != null && !configurationToUpdate.hasTestPackageRandomId(packageRandomId)) {
							configurationToUpdate.addTestPackageRandomId(packageRandomId);
							// male
							if (trueGender == 0) {
								configurationToUpdate.incrementMaleOverall();
								if (predictedGender == 0) {
									configurationToUpdate.incrementMaleCorrect();
								}
								// female
							} else {
								configurationToUpdate.incrementFemaleOverall();
								if (predictedGender == 1) {
									configurationToUpdate.incrementFemaleCorrect();
								}
							}
						}
					} finally {
						testWeightsConfigurationsLock.readLock().unlock();
					}
				} else {
					JsonElement updateValueJsonElement = objectReceived.get("v");
					// train package
					if (updateValueJsonElement != null) {
						int index = objectReceived.get("i").getAsInt();
						int value = updateValueJsonElement.getAsInt();
						packageReceived = new TrainPackage(
								svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()),
								index, value);
						packageReceived.setAssociatedDbStatement(trainPackageInsertStatement);

						trainWeightsConfigurationsLock.readLock().lock();
						try {
							TrainWeightsConfiguration configurationToUpdate =
									trainConfigurations.get(new ServerRequestId(svmId, iteration));
							// <package not outdated> && <no duplicate>
							if (configurationToUpdate != null && !configurationToUpdate.hasTrainPackageRandomId(packageRandomId)) {
								configurationToUpdate.addTrainPackageRandomId(packageRandomId);
									if (value == 0) {
										configurationToUpdate.decrementGradientNotNormalizedByIndex(index);
									} else {
										configurationToUpdate.incrementGradientNotNormalizedByIndex(index);
									}
							}
						} finally {
							trainWeightsConfigurationsLock.readLock().unlock();
						}
						// participation package
					} else {
						packageReceived = new ParticipationPackage(
								svmId, iteration, packageRandomId, new Timestamp(System.currentTimeMillis()));
						packageReceived.setAssociatedDbStatement(participationPackageInsertStatement);

						trainWeightsConfigurationsLock.readLock().lock();
						try {
							TrainWeightsConfiguration configurationToUpdate =
									trainConfigurations.get(new ServerRequestId(svmId, iteration));
							// <package not outdated> && <no duplicate>
							if (configurationToUpdate != null && !configurationToUpdate.hasParticipationPackageRandomId(packageRandomId)) {
								configurationToUpdate.addParticipationPackageRandomId(packageRandomId);
									configurationToUpdate.incrementNumParticipants();
							}
						} finally {
							trainWeightsConfigurationsLock.readLock().unlock();
						}
					}
				}

				packageLoggingExecutor.submit(new DatabaseLogger(packageReceived));
			} catch (IOException e) {
				e.printStackTrace();
			// For debugging. Otherwise RuntimeExceptions would go unnoticed.
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
                httpExchange.getResponseHeaders().set("Content-Type", "appication/json");
                httpExchange.sendResponseHeaders(200, PACKAGE_RESPONSE.length());
                OutputStream httpExchangeOutputStream = httpExchange.getResponseBody();
                httpExchangeOutputStream.write(PACKAGE_RESPONSE.getBytes());
                httpExchangeOutputStream.close();
				httpExchange.close();
			}
		}

	}

}
