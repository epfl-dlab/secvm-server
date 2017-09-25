package secvm_server;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TrainWeightsConfiguration extends WeightsConfiguration {
	private int min_number_train_participants;
	private int num_participants;
	private float lambda;
	private List<Integer> train_outcomes_dice_roll;
	
	// the weight vector that is being sent to the users for training
	private List<Float> weightsToUseForTraining;
	// the weight vector that is being updated by the users
	// AtomicReferenceArray instead of regular array for concurrent updates
	private AtomicReferenceArray<Float> weightsBeingTrained;
	
	public TrainWeightsConfiguration() {
		super();
	}
	
	public TrainWeightsConfiguration(int svmId, int iteration, int numBins, List<Float> diceRollProbabilities, List<FeatureVectorProperties> features,
			int min_number_train_participants, int num_participants, float lambda, List<Integer> train_outcomes_dice_roll,
			List<Float> weightsToUseForTraining, AtomicReferenceArray<Float> weightsBeingTrained) {
		super(svmId, iteration, numBins, diceRollProbabilities, features);
		this.min_number_train_participants = min_number_train_participants;
		this.num_participants = num_participants;
		this.lambda = lambda;
		this.train_outcomes_dice_roll = train_outcomes_dice_roll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.weightsBeingTrained = weightsBeingTrained;
	}
	
	public int getMin_number_train_participants() {
		return min_number_train_participants;
	}

	public void setMin_number_train_participants(int min_number_train_participants) {
		this.min_number_train_participants = min_number_train_participants;
	}

	public int getNum_participants() {
		return num_participants;
	}

	public void setNum_participants(int num_participants) {
		this.num_participants = num_participants;
	}

	public float getLambda() {
		return lambda;
	}

	public void setLambda(float lambda) {
		this.lambda = lambda;
	}

	public List<Integer> getTrain_outcomes_dice_roll() {
		return train_outcomes_dice_roll;
	}

	public void setTrain_outcomes_dice_roll(List<Integer> train_outcomes_dice_roll) {
		this.train_outcomes_dice_roll = train_outcomes_dice_roll;
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
