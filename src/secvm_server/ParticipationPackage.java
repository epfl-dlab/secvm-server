package secvm_server;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ParticipationPackage extends UserPackage {
	
	public ParticipationPackage(String experimentId, String packageId, String features) {
		super(experimentId, packageId, features);
	}

	@Override
	public PreparedStatement fillStatement(PreparedStatement statement)
			throws SQLException {
		statement.setString(1, experimentId);
		statement.setString(2, packageId);
		statement.setString(3, features);
		return statement;
	}

}
