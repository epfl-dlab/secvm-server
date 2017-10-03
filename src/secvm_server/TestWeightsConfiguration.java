package secvm_server;

import java.util.List;

public class TestWeightsConfiguration extends WeightsConfiguration {
	private List<Integer> testOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for testing
	private List<Float> weightsToUseForTesting;
	
	private int femaleOverall;
	private int maleOverall;
	private int femaleCorrect;
	private int maleCorrect;

	public TestWeightsConfiguration() {
		super();
	}

	public TestWeightsConfiguration(List<Integer> testOutcomesDiceRoll, List<Float> weightsToUseForTesting,
			int femaleOverall, int maleOverall, int femaleCorrect, int maleCorrect) {
		super();
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
		this.femaleOverall = femaleOverall;
		this.maleOverall = maleOverall;
		this.femaleCorrect = femaleCorrect;
		this.maleCorrect = maleCorrect;
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

	public int getFemaleOverall() {
		return femaleOverall;
	}

	public void setFemaleOverall(int femaleOverall) {
		this.femaleOverall = femaleOverall;
	}

	public int getMaleOverall() {
		return maleOverall;
	}

	public void setMaleOverall(int maleOverall) {
		this.maleOverall = maleOverall;
	}

	public int getFemaleCorrect() {
		return femaleCorrect;
	}

	public void setFemaleCorrect(int femaleCorrect) {
		this.femaleCorrect = femaleCorrect;
	}

	public int getMaleCorrect() {
		return maleCorrect;
	}

	public void setMaleCorrect(int maleCorrect) {
		this.maleCorrect = maleCorrect;
	}
}
