package com.cliqz.secvmserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public enum SqlQueries {

	INSERT_INTO_PACKAGE_PARTICIPATION_TABLE(
			"INSERT INTO package_participation VALUES (NULL, ?, ?, ?, ?)"),
	
	INSERT_INTO_PACKAGE_TRAIN_TABLE(
			"INSERT INTO package_train VALUES (NULL, ?, ?, ?, ?, ?, ?)"),
	
	INSERT_INTO_PACKAGE_TEST_TABLE(
			"INSERT INTO package_test VALUES (NULL, ?, ?, ?, ?, ?, ?)"),
	
	INSERT_INTO_WEIGHTS_TABLE(
			"INSERT INTO weight_vector VALUES(?, ?, ?, NULL, ?, ?, ?)"),
	
	INSERT_INTO_TEST_ACCURACY_TABLE(
			"INSERT INTO test_accuracy VALUES(?, ?, ?, ?, ?, ?)"),
	
	UPDATE_WEIGHTS(
			"UPDATE weight_vector SET weights = ? WHERE svm_id = ? AND iteration = ?"),
	
	UPDATE_GRADIENT(
			"UPDATE weight_vector SET gradient_not_normalized = ? WHERE svm_id = ? AND iteration = ?"),
	
	UPDATE_TRAIN_END_TIME(
			"UPDATE weight_vector SET training_end_time = ? WHERE svm_id = ? AND iteration = ?"),
	
	UPDATE_GRADIENT_NUM_PARTICIPANTS(
			"UPDATE weight_vector SET gradient_not_normalized = ?, num_participants = ? WHERE svm_id = ? AND iteration = ?"),
	
	UPDATE_TEST_RESULTS(
			"UPDATE test_accuracy SET female_overall = ?, male_overall = ?, female_correct = ?, male_correct = ?\n" +
			"WHERE svm_id = ? AND iteration = ?"),
	
	GET_TRAIN_CONFIGURATIONS(
			"SELECT svm.svm_id, svm.min_number_train_participants, weight_vector.num_participants," + 
			"	svm.lambda, svm.num_bins, dice_roll.probabilities, dice_roll.id, svm.train_outcomes_dice_roll," +
			"	feature_vector.feature_type, feature_vector.id, feature_vector.num_features," + 
			"	feature_vector.num_hashes, weight_vector.weights, weight_vector.iteration," +
			"	weight_vector.gradient_not_normalized\n" +
			"FROM svm, weight_vector, svm_uses_feature_vector, feature_vector, dice_roll\n" + 
			"WHERE svm.train_enabled = 1 AND svm.svm_id = weight_vector.svm_id" +
			"	AND svm.svm_id = svm_uses_feature_vector.svm_id" +
			"	AND svm_uses_feature_vector.feature_vector_id = feature_vector.id" +
			"	AND svm.dice_roll_id = dice_roll.id\n" + 
			"ORDER BY svm.svm_id, weight_vector.iteration DESC"),
	
	GET_TEST_CONFIGURATIONS(
			"SELECT svm.svm_id, dice_roll.probabilities, dice_roll.id, svm.test_outcomes_dice_roll," + 
			"	feature_vector.feature_type, feature_vector.id, feature_vector.num_features," + 
			"	feature_vector.num_hashes, weight_vector.weights, weight_vector.iteration\n" + 
			"FROM svm, weight_vector, svm_uses_feature_vector, feature_vector, dice_roll\n" + 
			"WHERE svm.test_enabled = 1 AND svm.svm_id = weight_vector.svm_id" +
			"	AND svm.svm_id = svm_uses_feature_vector.svm_id" +
			"	AND svm_uses_feature_vector.feature_vector_id = feature_vector.id" +
			"	AND svm.dice_roll_id = dice_roll.id" +
			"	AND (weight_vector.num_participants >= svm.min_number_train_participants" +
			"		OR weight_vector.iteration = 0)\n" +
			"ORDER BY svm.svm_id, weight_vector.iteration DESC"),
	
	GET_TEST_ACCURACY(
			"SELECT female_overall, male_overall, female_correct, male_correct\n" +
			"FROM test_accuracy\n" +
			"WHERE svm_id = ? AND iteration = ?"),
	
	GET_PARTICIPATION_PACKAGE_RANDOM_IDS(
			"SELECT DISTINCT package_random_id\n" +
			"FROM package_participation\n" +
			"WHERE svm_id = ? AND iteration = ?"),
	
	GET_TRAIN_PACKAGE_RANDOM_IDS(
			"SELECT DISTINCT package_random_id\n" +
			"FROM package_train\n" +
			"WHERE svm_id = ? AND iteration = ?"),
	
	GET_TEST_PACKAGE_RANDOM_IDS(
			"SELECT DISTINCT package_random_id\n" +
			"FROM package_test\n" +
			"WHERE svm_id = ? AND iteration = ?");
	
	public final String query;
	
	SqlQueries(String query) {
		this.query = query;
	}
	
	public PreparedStatement createPreparedStatement(Connection dbConnection)
			throws SQLException {
		return dbConnection.prepareStatement(this.query);
	}
}
