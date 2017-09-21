package secvm_server;

public class TestWeightsConfiguration extends WeightsConfiguration {
	private int[] testOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for testing
	private float[] weightsToUseForTesting;

	public TestWeightsConfiguration(int svmId, int numBins, float[] diceRollProbabilities, Features[] features,
			int[] testOutcomesDiceRoll, float[] weightsToUseForTesting) {
		super(svmId, numBins, diceRollProbabilities, features);
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
	}

	public int[] getTestOutcomesDiceRoll() {
		return testOutcomesDiceRoll;
	}

	public float[] getWeightsToUseForTesting() {
		return weightsToUseForTesting;
	}
}
