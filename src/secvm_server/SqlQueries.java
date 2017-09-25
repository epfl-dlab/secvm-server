package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public enum SqlQueries {

	INSERT_INTO_PARTICIPATION_DB(
			"INSERT INTO participation VALUES(?, ?, ?, UNIX_TIMESTAMP(NOW(6)))"),
	
	GET_TRAIN_CONFIGURATIONS(
			"SELECT svm.svm_id, svm.min_number_train_participants, weight_vector.num_participants," + 
			"	svm.lambda, svm.num_bins, dice_roll.probabilities, svm.train_outcomes_dice_roll," +
			"	feature_vector.feature_type, feature_vector.num_features," + 
			"	feature_vector.num_hashes, weight_vector.weights, weight_vector.iteration\n" + 
			"FROM svm, weight_vector, svm_uses_feature_vector, feature_vector, dice_roll\n" + 
			"WHERE svm.train_enabled = 1 AND svm.svm_id = weight_vector.svm_id" +
			"	AND svm.svm_id = svm_uses_feature_vector.svm_id" +
			"	AND svm_uses_feature_vector.feature_vector_id = feature_vector.id" +
			"	AND svm.dice_roll_id = dice_roll.id\n" + 
			"ORDER BY svm.svm_id, weight_vector.iteration DESC"),
	
	GET_TEST_CONFIGURATIONS(
			"SELECT svm.svm_id, dice_roll.probabilities, svm.test_outcomes_dice_roll" + 
			"	feature_vector.feature_type, feature_vector.num_features," + 
			"	feature_vector.num_hashes, weight_vector.weights, weight_vector.iteration\n" + 
			"FROM svm, weight_vector, svm_uses_feature_vector, feature_vector, dice_roll\n" + 
			"WHERE svm.test_enabled = 1 AND svm.svm_id = weight_vector.svm_id" + 
			"	AND svm.svm_id = svm_uses_feature_vector.svm_id" + 
			"	AND svm_uses_feature_vector.feature_vector_id = feature_vector.id" +
			"	AND svm.dice_roll_id = dice_roll.id" +
			"	AND (weight_vector.num_participants >= svm.min_number_train_participants" +
			"		OR weight_vector.iteration = 0)\n" +
			"ORDER BY svm.svm_id, weight_vector.iteration DESC");
	
	public final String query;
	
	SqlQueries(String query) {
		this.query = query;
	}
	
	public PreparedStatement createPreparedStatement(Connection dbConnection)
			throws SQLException {
		return dbConnection.prepareStatement(this.query);
	}
}
