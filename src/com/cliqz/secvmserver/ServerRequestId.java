package com.cliqz.secvmserver;

/**
 * Indicates to which request from the server (i.e. to which svm)
 * a train, test or participation package from a user belongs.
 */
public class ServerRequestId {
	private int svmId;
	private int iteration;
	
	public ServerRequestId(int svmId, int iteration) {
		this.svmId = svmId;
		this.iteration = iteration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + iteration;
		result = prime * result + svmId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerRequestId other = (ServerRequestId) obj;
		if (iteration != other.iteration)
			return false;
		if (svmId != other.svmId)
			return false;
		return true;
	}
}
