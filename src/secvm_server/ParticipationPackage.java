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
		return super.fillStatement(statement);
	}

}
