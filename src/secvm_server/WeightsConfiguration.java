package secvm_server;

/**
 * Contains the information about a weight vector that is going to be sent to the users.
 *
 */
public class WeightsConfiguration {
	// maybe remove this and only have as key in the Map
	private int svmId;
	private int iteration;
	private int numBins;
	private float[] diceRollProbabilities;
	// if (features.length == 2), then use the merged vector
	private Features[] features;
	
	public WeightsConfiguration(int svmId, int iteration, int numBins, float[] diceRollProbabilities, Features[] features) {
		this.svmId = svmId;
		this.iteration = iteration;
		this.numBins = numBins;
		this.diceRollProbabilities = diceRollProbabilities;
		this.features = features;
	}

	public int getSvmId() {
		return svmId;
	}
	
	public int getIteration() {
		return iteration;
	}

	public int getNumBins() {
		return numBins;
	}

	public float[] getDiceRollProbabilities() {
		return diceRollProbabilities;
	}

	public Features[] getFeatures() {
		return features;
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
		WeightsConfiguration other = (WeightsConfiguration) obj;
		if (iteration != other.iteration)
			return false;
		if (svmId != other.svmId)
			return false;
		return true;
	}


	public static class Features {
		private String featureType;
		private int numFeatures;
		private int numHashes;
		
		public Features(String featureType, int numFeatures, int numHashes) {
			this.featureType = featureType;
			this.numFeatures = numFeatures;
			this.numHashes = numHashes;
		}

		public String getFeature_type() {
			return featureType;
		}

		public int getNum_features() {
			return numFeatures;
		}

		public int getNum_hashes() {
			return numHashes;
		}
		
	}
}
