package com.cliqz.secvmserver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TestPackage extends UserPackage {
	
	private int trueGender;
	private int predictedGender;
	
	public TestPackage(int svmId, int iteration, String packageRandomId, Timestamp arrivalTime, int trueGender, int predictedGender) {
		super(svmId, iteration, packageRandomId, arrivalTime);
		this.trueGender = trueGender;
		this.predictedGender = predictedGender;
	}

	@Override
	public PreparedStatement fillStatement(PreparedStatement statement) throws SQLException {
		super.fillStatement(statement);
		statement.setInt(5, trueGender);
		statement.setInt(6, predictedGender);
		return statement;
	}

}
