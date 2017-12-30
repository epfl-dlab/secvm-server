package com.cliqz.secvmserver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ParticipationPackage extends UserPackage {
	
	public ParticipationPackage(int svmId, int iteration, String packageRandomId, Timestamp arrivalTime) {
		super(svmId, iteration, packageRandomId, arrivalTime);
	}

	@Override
	public PreparedStatement fillStatement(PreparedStatement statement)
			throws SQLException {
		return super.fillStatement(statement);
	}

}
