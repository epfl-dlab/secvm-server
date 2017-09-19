package secvm_server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Data packages arriving from the users.
 */
public abstract class UserPackage {
	
	protected String experimentId;
	protected String packageId;
	protected String features;
	
	public UserPackage(String experimentId, String packageId, String features) {
		this.experimentId = experimentId;
		this.packageId = packageId;
		this.features = features;
	}
	
	/**
	 * Populates a PreparedStatement with the attributes of this UserPackage.
	 * @param statement the statement to be populated
	 * @return the same statement, now populated
	 */
	public abstract PreparedStatement fillStatement(PreparedStatement statement)
			throws SQLException;
}
