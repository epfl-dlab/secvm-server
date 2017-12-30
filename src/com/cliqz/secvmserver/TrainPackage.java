package com.cliqz.secvmserver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TrainPackage extends UserPackage {
	
	private int index;
	private int value;
	
	public TrainPackage(int svmId, int iteration, String packageRandomId, Timestamp arrivalTime, int index, int value) {
		super(svmId, iteration, packageRandomId, arrivalTime);
		this.index = index;
		this.value = value;
	}

	@Override
	public PreparedStatement fillStatement(PreparedStatement statement) throws SQLException {
		super.fillStatement(statement);
		statement.setInt(5, index);
		statement.setInt(6, value);
		return statement;
	}

}
