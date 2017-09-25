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
	// the weight vector that is being updated by the users
	// AtomicReferenceArray instead of regular array for concurrent updates
	private AtomicReferenceArray<Float> weightsBeingTrained;
	
	public TrainWeightsConfiguration() {
		super();
	}
	
	public TrainWeightsConfiguration(int svmId, int iteration, int numBins, List<Float> diceRollProbabilities, List<FeatureVectorProperties> features,
			int minNumberTrainParticipants, int numParticipants, float lambda, List<Integer> trainOutcomesDiceRoll,
			List<Float> weightsToUseForTraining, AtomicReferenceArray<Float> weightsBeingTrained) {
		super(svmId, iteration, numBins, diceRollProbabilities, features);
		this.minNumberTrainParticipants = minNumberTrainParticipants;
		this.numParticipants = numParticipants;
		this.lambda = lambda;
		this.trainOutcomesDiceRoll = trainOutcomesDiceRoll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.weightsBeingTrained = weightsBeingTrained;
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

	public AtomicReferenceArray<Float> getWeightsBeingTrained() {
		return weightsBeingTrained;
	}

	public void setWeightsBeingTrained(AtomicReferenceArray<Float> weightsBeingTrained) {
		this.weightsBeingTrained = weightsBeingTrained;
	}

	
	// as opposed to the other getters/setters, these two work index-based
	public Float getWeightsBeingTrainedByIndex(int index) {
		return weightsBeingTrained.get(index);
	}

	public void setWeightsBeingTrainedByIndex(int index, Float value) {
		this.weightsBeingTrained.set(index, value);
	}
}
