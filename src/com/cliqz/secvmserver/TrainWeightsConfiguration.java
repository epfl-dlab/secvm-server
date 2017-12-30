package com.cliqz.secvmserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;


/*
 * numParticipants is atomic and the getter and setter work atomically on it.
 */
public class TrainWeightsConfiguration extends WeightsConfiguration {
	private int minNumberTrainParticipants;
	private AtomicInteger numParticipants;
	private float lambda;
	private List<Integer> trainOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for training
	private List<Float> weightsToUseForTraining;
	// the gradient that is being updated by the users; not yet divided by the number of users 
	// AtomicReferenceArray instead of regular array for concurrent updates
	private AtomicIntegerArray gradientNotNormalized;
	
	// The server has already received participation/train packages with these random ids.
	// If new ones with one of those ids come in, they are most likely duplicates and will
	// be ignored.
	private Set<String> participationPackageRandomIdsAlreadyReceived;
	private Set<String> trainPackageRandomIdsAlreadyReceived;
	
	public TrainWeightsConfiguration() {
		super();
		numParticipants = new AtomicInteger();
		participationPackageRandomIdsAlreadyReceived = new HashSet<>();
		trainPackageRandomIdsAlreadyReceived = new HashSet<>();
	}
	
	public TrainWeightsConfiguration(int svmId, int iteration, int numBins, int diceRollId,
			List<Float> diceRollProbabilities, List<FeatureVectorProperties> features, int minNumberTrainParticipants,
			AtomicInteger numParticipants, float lambda, List<Integer> trainOutcomesDiceRoll,
			List<Float> weightsToUseForTraining, AtomicIntegerArray gradientNotNormalized,
			Set<String> participationPackageRandomIdsAlreadyReceived,
			Set<String> trainPackageRandomIdsAlreadyReceived) {
		super(svmId, iteration, numBins, diceRollId, diceRollProbabilities, features);
		this.minNumberTrainParticipants = minNumberTrainParticipants;
		this.numParticipants = numParticipants;
		this.lambda = lambda;
		this.trainOutcomesDiceRoll = trainOutcomesDiceRoll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.gradientNotNormalized = gradientNotNormalized;
		this.participationPackageRandomIdsAlreadyReceived = participationPackageRandomIdsAlreadyReceived;
		this.trainPackageRandomIdsAlreadyReceived = trainPackageRandomIdsAlreadyReceived;
	}

	public int getMinNumberTrainParticipants() {
		return minNumberTrainParticipants;
	}

	public void setMinNumberTrainParticipants(int minNumberTrainParticipants) {
		this.minNumberTrainParticipants = minNumberTrainParticipants;
	}

	public int getNumParticipants() {
		return numParticipants.get();
	}

	public void setNumParticipants(int numParticipants) {
		this.numParticipants.set(numParticipants);
	}

	public float getLambda() {
		return lambda;
	}

	public void setLambda(float lambda) {
		this.lambda = lambda;
	}

	public List<Integer> getTrainOutcomesDiceRoll() {
		return trainOutcomesDiceRoll;
	}

	public void setTrainOutcomesDiceRoll(List<Integer> trainOutcomesDiceRoll) {
		this.trainOutcomesDiceRoll = trainOutcomesDiceRoll;
	}

	public List<Float> getWeightsToUseForTraining() {
		return weightsToUseForTraining;
	}

	public void setWeightsToUseForTraining(List<Float> weightsToUseForTraining) {
		this.weightsToUseForTraining = weightsToUseForTraining;
	}

	public AtomicIntegerArray getGradientNotNormalized() {
		return gradientNotNormalized;
	}

	public void setGradientNotNormalized(AtomicIntegerArray gradientNotNormalized) {
		this.gradientNotNormalized = gradientNotNormalized;
	}
	
	public Set<String> getParticipationPackageRandomIdsAlreadyReceived() {
		return participationPackageRandomIdsAlreadyReceived;
	}

	public void setParticipationPackageRandomIdsAlreadyReceived(Set<String> participationPackageRandomIdsAlreadyReceived) {
		this.participationPackageRandomIdsAlreadyReceived = participationPackageRandomIdsAlreadyReceived;
	}

	public Set<String> getTrainPackageRandomIdsAlreadyReceived() {
		return trainPackageRandomIdsAlreadyReceived;
	}

	public void setTrainPackageRandomIdsAlreadyReceived(Set<String> trainPackageRandomIdsAlreadyReceived) {
		this.trainPackageRandomIdsAlreadyReceived = trainPackageRandomIdsAlreadyReceived;
	}

	
	// as opposed to the other getters/setters, these two work index-based
	public Integer getGradientNotNormalizedByIndex(int index) {
		return gradientNotNormalized.get(index);
	}

	public void setGradientNotNormalizedByIndex(int index, Integer value) {
		this.gradientNotNormalized.set(index, value);
	}
	
	public void incrementGradientNotNormalizedByIndex(int index) {
		this.gradientNotNormalized.getAndIncrement(index);
	}
	
	public void decrementGradientNotNormalizedByIndex(int index) {
		this.gradientNotNormalized.getAndDecrement(index);
	}
	
	
	public void incrementNumParticipants() {
		numParticipants.getAndIncrement();
	}
	
	
	public void addParticipationPackageRandomId(String id) {
		participationPackageRandomIdsAlreadyReceived.add(id);
	}
	
	public void addTrainPackageRandomId(String id) {
		trainPackageRandomIdsAlreadyReceived.add(id);
	}
	
	public boolean hasParticipationPackageRandomId(String id) {
		return participationPackageRandomIdsAlreadyReceived.contains(id);
	}
	
	public boolean hasTrainPackageRandomId(String id) {
		return trainPackageRandomIdsAlreadyReceived.contains(id);
	}
}
