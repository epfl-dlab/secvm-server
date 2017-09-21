package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public enum SqlQueries {

	INSERT_INTO_PARTICIPATION_DB(
			"INSERT INTO participation VALUES(?, ?, ?, UNIX_TIMESTAMP(NOW(6)))");
	
	public final String query;
	
	SqlQueries(String query) {
		this.query = query;
	}
	
	public PreparedStatement createPreparedStatement(Connection dbConnection)
			throws SQLException {
		return dbConnection.prepareStatement(this.query);
	}
}
