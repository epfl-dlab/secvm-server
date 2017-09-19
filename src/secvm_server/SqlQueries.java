package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public enum SqlQueries {
	// TODO: fill the two tables below and use foreign keys in the other tables
//	"CREATE TABLE IF NOT EXISTS svms ( " +
//	"	svm_name TEXT, " +
//	"	experiment_id TEXT, " +
//	"	PRIMARY KEY svm_name" +
//	")"
//	
//	"CREATE TABLE IF NOT EXISTS features_used ( " +
//	"	experiment_id TEXT, " +
//	"	features TEXT, " +
//	"	PRIMARY KEY experiment_id" +
//	")"
			
	CREATE_PARTICIPATION_DB (
			"CREATE TABLE IF NOT EXISTS participation (" + 
			"	experiment_id TEXT NOT NULL, " + 
			"	package_id TEXT NOT NULL, " + 
			"	features TEXT NOT NULL, " + 
			" 	PRIMARY KEY (experiment_id, package_id, features)" + 
			")"),
	
	INSERT_INTO_PARTICIPATION_DB(
			"INSERT INTO participation VALUES(?, ?, ?)");
	
	public final String query;
	
	SqlQueries(String query) {
		this.query = query;
	}
	
	public PreparedStatement createPreparedStatement(Connection dbConnection)
			throws SQLException {
		return dbConnection.prepareStatement(this.query);
	}
}
