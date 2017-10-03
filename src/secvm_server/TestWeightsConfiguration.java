package secvm_server;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * femaleOverall, maleOverall, femaleCorrect, maleCorrect are all atomic and the
 * getters and setters work atomically on them.
 */
public class TestWeightsConfiguration extends WeightsConfiguration {
	private List<Integer> testOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for testing
	private List<Float> weightsToUseForTesting;
	
	private AtomicInteger femaleOverall;
	private AtomicInteger maleOverall;
	private AtomicInteger femaleCorrect;
	private AtomicInteger maleCorrect;

	public TestWeightsConfiguration() {
		super();
		femaleOverall = new AtomicInteger();
		maleOverall = new AtomicInteger();
		femaleCorrect = new AtomicInteger();
		maleCorrect = new AtomicInteger();
	}

	public TestWeightsConfiguration(List<Integer> testOutcomesDiceRoll, List<Float> weightsToUseForTesting,
			int femaleOverall, int maleOverall, int femaleCorrect, int maleCorrect) {
		super();
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
		this.femaleOverall = new AtomicInteger(femaleOverall);
		this.maleOverall = new AtomicInteger(maleOverall);
		this.femaleCorrect = new AtomicInteger(femaleCorrect);
		this.maleCorrect = new AtomicInteger(maleCorrect);
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
		return femaleOverall.get();
	}

	public void setFemaleOverall(int femaleOverall) {
		this.femaleOverall.set(femaleOverall);
	}

	public int getMaleOverall() {
		return maleOverall.get();
	}

	public void setMaleOverall(int maleOverall) {
		this.maleOverall.set(maleOverall);
	}

	public int getFemaleCorrect() {
		return femaleCorrect.get();
	}

	public void setFemaleCorrect(int femaleCorrect) {
		this.femaleCorrect.set(femaleCorrect);
	}

	public int getMaleCorrect() {
		return maleCorrect.get();
	}

	public void setMaleCorrect(int maleCorrect) {
		this.maleCorrect.set(maleCorrect);
	}
}
