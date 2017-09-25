package secvm_server;

import java.util.List;

public class TestWeightsConfiguration extends WeightsConfiguration {
	private List<Integer> testOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for testing
	private List<Float> weightsToUseForTesting;

	public TestWeightsConfiguration(int svmId, int numBins, float[] diceRollProbabilities, Features[] features,
			List<Integer> testOutcomesDiceRoll, List<Float> weightsToUseForTesting) {
		super(svmId, numBins, diceRollProbabilities, features);
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
	}

	public List<Integer> getTestOutcomesDiceRoll() {
		return testOutcomesDiceRoll;
	}

	public List<Float> getWeightsToUseForTesting() {
		return weightsToUseForTesting;
	}
}
