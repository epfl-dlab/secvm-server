package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Data packages arriving from the users.
 */
public abstract class UserPackage {
	
	protected int svmId;
	protected int iteration;
	protected String packageRandomId;
	protected Timestamp arrivalTime;
	
	public UserPackage(int svmId, int iteration, String packageRandomId, Timestamp arrivalTime) {
		this.svmId = svmId;
		this.iteration = iteration;
		this.packageRandomId = packageRandomId;
		this.arrivalTime = arrivalTime;
	}

	/**
	 * Populates a PreparedStatement with the attributes of this UserPackage.
	 * @param statement the statement to be populated
	 * @return the same statement, now populated
	 */
	public PreparedStatement fillStatement(PreparedStatement statement)
			throws SQLException {
		statement.setInt(1, svmId);
		statement.setInt(2, iteration);
		statement.setString(3, packageRandomId);
		statement.setTimestamp(4, arrivalTime);
		return statement;
	}
}
