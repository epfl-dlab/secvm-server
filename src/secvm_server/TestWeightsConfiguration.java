package secvm_server;

import java.util.List;

public class TestWeightsConfiguration extends WeightsConfiguration {
	private List<Integer> testOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for testing
	private List<Float> weightsToUseForTesting;

	public TestWeightsConfiguration() {
		super();
	}
	
	public TestWeightsConfiguration(int svmId, int iteration, int numBins, List<Float> diceRollProbabilities,
			List<Features> features, List<Integer> testOutcomesDiceRoll, List<Float> weightsToUseForTesting) {
		super(svmId, iteration, numBins, diceRollProbabilities, features);
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
	}

	public List<Integer> getTestOutcomesDiceRoll() {
		return testOutcomesDiceRoll;
	}

	public void setTestOutcomesDiceRoll(List<Integer> testOutcomesDiceRoll) {
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
	}

	public List<Float> getWeightsToUseForTesting() {
		return weightsToUseForTesting;
	}

	public void setWeightsToUseForTesting(List<Float> weightsToUseForTesting) {
		this.weightsToUseForTesting = weightsToUseForTesting;
	}
}
