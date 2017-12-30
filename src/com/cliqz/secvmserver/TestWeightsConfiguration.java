package com.cliqz.secvmserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	
	// The server has already received test packages with these random ids.
	// If new ones with one of those ids come in, they are most likely duplicates and will
	// be ignored.
	private Set<String> testPackageRandomIdsAlreadyReceived;

	public TestWeightsConfiguration() {
		super();
		femaleOverall = new AtomicInteger();
		maleOverall = new AtomicInteger();
		femaleCorrect = new AtomicInteger();
		maleCorrect = new AtomicInteger();
		
		testPackageRandomIdsAlreadyReceived = new HashSet<>();
	}

	public TestWeightsConfiguration(int svmId, int iteration, int numBins, int diceRollId,
			List<Float> diceRollProbabilities, List<FeatureVectorProperties> features,
			List<Integer> testOutcomesDiceRoll, List<Float> weightsToUseForTesting, AtomicInteger femaleOverall,
			AtomicInteger maleOverall, AtomicInteger femaleCorrect, AtomicInteger maleCorrect,
			Set<String> testPackageRandomIdsAlreadyReceived) {
		super(svmId, iteration, numBins, diceRollId, diceRollProbabilities, features);
		this.testOutcomesDiceRoll = testOutcomesDiceRoll;
		this.weightsToUseForTesting = weightsToUseForTesting;
		this.femaleOverall = femaleOverall;
		this.maleOverall = maleOverall;
		this.femaleCorrect = femaleCorrect;
		this.maleCorrect = maleCorrect;
		this.testPackageRandomIdsAlreadyReceived = testPackageRandomIdsAlreadyReceived;
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
	
	public Set<String> getTestPackageRandomIdsAlreadyReceived() {
		return testPackageRandomIdsAlreadyReceived;
	}

	public void setTestPackageRandomIdsAlreadyReceived(Set<String> testPackageRandomIdsAlreadyReceived) {
		this.testPackageRandomIdsAlreadyReceived = testPackageRandomIdsAlreadyReceived;
	}
	

	public void incrementFemaleOverall() {
		femaleOverall.getAndIncrement();
	}
	
	public void incrementMaleOverall() {
		maleOverall.getAndIncrement();
	}
	
	public void incrementFemaleCorrect() {
		femaleCorrect.getAndIncrement();
	}
	
	public void incrementMaleCorrect() {
		maleCorrect.getAndIncrement();
	}
	
	public void addTestPackageRandomId(String id) {
		testPackageRandomIdsAlreadyReceived.add(id);
	}
	
	public boolean hasTestPackageRandomId(String id) {
		return testPackageRandomIdsAlreadyReceived.contains(id);
	}
}
