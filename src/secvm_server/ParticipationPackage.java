package secvm_server;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ParticipationPackage extends UserPackage {
	
	public ParticipationPackage(int svmId, int iteration, String packageRandomId) {
		super(svmId, iteration, packageRandomId);
	}

	@Override
	public PreparedStatement fillStatement(PreparedStatement statement)
			throws SQLException {
		statement.setInt(1, svmId);
		statement.setInt(2, iteration);
		statement.setString(3, packageRandomId);
		return statement;
	}

}
