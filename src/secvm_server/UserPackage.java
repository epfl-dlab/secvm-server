package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Data packages arriving from the users.
 */
public abstract class UserPackage {
	
	protected int svmId;
	protected int iteration;
	protected String packageRandomId;
	
	public UserPackage(int svmId, int iteration, String packageRandomId) {
		this.svmId = svmId;
		this.iteration = iteration;
		this.packageRandomId = packageRandomId;
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
		return statement;
	}
}
