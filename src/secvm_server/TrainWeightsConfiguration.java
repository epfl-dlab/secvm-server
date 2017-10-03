package secvm_server;

import java.util.List;
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
	
	public TrainWeightsConfiguration() {
		super();
		numParticipants = new AtomicInteger();
	}
	
	public TrainWeightsConfiguration(int svmId, int iteration, int numBins, List<Float> diceRollProbabilities, List<FeatureVectorProperties> features,
			int minNumberTrainParticipants, int numParticipants, float lambda, List<Integer> trainOutcomesDiceRoll,
			List<Float> weightsToUseForTraining, AtomicIntegerArray gradientNotNormalized) {
		super(svmId, iteration, numBins, diceRollProbabilities, features);
		this.minNumberTrainParticipants = minNumberTrainParticipants;
		this.numParticipants = new AtomicInteger(numParticipants);
		this.lambda = lambda;
		this.trainOutcomesDiceRoll = trainOutcomesDiceRoll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.gradientNotNormalized = gradientNotNormalized;
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
	
}
