package secvm_server;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TrainWeightsConfiguration extends WeightsConfiguration {
	private int minNumberTrainParticipants;
	private int numParticipants;
	private float lambda;
	private List<Integer> trainOutcomesDiceRoll;
	
	// the weight vector that is being sent to the users for training
	private List<Float> weightsToUseForTraining;
	// the gradient that is being updated by the users; not yet divided by the number of users 
	// AtomicReferenceArray instead of regular array for concurrent updates
	private AtomicReferenceArray<Integer> gradientNotNormalized;
	// the number of vectors of which gradientNotNormalized is the sum, i.e. the number of users
	// that participated in the current update round
	private int numGradientUpdateVectors;
	
	public TrainWeightsConfiguration() {
		super();
	}
	
	public TrainWeightsConfiguration(int svmId, int iteration, int numBins, List<Float> diceRollProbabilities, List<FeatureVectorProperties> features,
			int minNumberTrainParticipants, int numParticipants, float lambda, List<Integer> trainOutcomesDiceRoll,
			List<Float> weightsToUseForTraining, AtomicReferenceArray<Integer> gradientNotNormalized,
			int numGradientUpdateVectors) {
		super(svmId, iteration, numBins, diceRollProbabilities, features);
		this.minNumberTrainParticipants = minNumberTrainParticipants;
		this.numParticipants = numParticipants;
		this.lambda = lambda;
		this.trainOutcomesDiceRoll = trainOutcomesDiceRoll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.gradientNotNormalized = gradientNotNormalized;
		this.numGradientUpdateVectors = numGradientUpdateVectors;
	}
	
	public int getMinNumberTrainParticipants() {
		return minNumberTrainParticipants;
	}

	public void setMinNumberTrainParticipants(int minNumberTrainParticipants) {
		this.minNumberTrainParticipants = minNumberTrainParticipants;
	}

	public int getNumParticipants() {
		return numParticipants;
	}

	public void setNumParticipants(int numParticipants) {
		this.numParticipants = numParticipants;
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

	public AtomicReferenceArray<Integer> getGradientNotNormalized() {
		return gradientNotNormalized;
	}

	public void setGradientNotNormalized(AtomicReferenceArray<Integer> gradientNotNormalized) {
		this.gradientNotNormalized = gradientNotNormalized;
	}

	public int getNumGradientUpdateVectors() {
		return numGradientUpdateVectors;
	}

	public void setNumGradientUpdateVectors(int numGradientUpdateVectors) {
		this.numGradientUpdateVectors = numGradientUpdateVectors;
	}

	// as opposed to the other getters/setters, these two work index-based
	public Integer getGradientNotNormalizedByIndex(int index) {
		return gradientNotNormalized.get(index);
	}

	public void setGradientNotNormalizedByIndex(int index, Integer value) {
		this.gradientNotNormalized.set(index, value);
	}
}
