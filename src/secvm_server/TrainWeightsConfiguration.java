package secvm_server;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class TrainWeightsConfiguration extends WeightsConfiguration {
	private int min_number_train_participants;
	private int num_participants;
	private float lambda;
	private int[] train_outcomes_dice_roll;
	
	// the weight vector that is being sent to the users for training
	private float[] weightsToUseForTraining;
	// the weight vector that is being updated by the users
	// AtomicReferenceArray instead of regular array for concurrent updates
	private AtomicReferenceArray<Float> weightsBeingTrained;
	
	public TrainWeightsConfiguration(int svmId, int numBins, float[] diceRollProbabilities, Features[] features,
			int min_number_train_participants, int num_participants, float lambda, int[] train_outcomes_dice_roll,
			float[] weightsToUseForTraining, AtomicReferenceArray<Float> weightsBeingTrained) {
		super(svmId, numBins, diceRollProbabilities, features);
		this.min_number_train_participants = min_number_train_participants;
		this.num_participants = num_participants;
		this.lambda = lambda;
		this.train_outcomes_dice_roll = train_outcomes_dice_roll;
		this.weightsToUseForTraining = weightsToUseForTraining;
		this.weightsBeingTrained = weightsBeingTrained;
	}

	public int getNum_participants() {
		return num_participants;
	}

	public void setNum_participants(int num_participants) {
		this.num_participants = num_participants;
	}

	public int getMin_number_train_participants() {
		return min_number_train_participants;
	}

	public float getLambda() {
		return lambda;
	}

	public int[] getTrain_outcomes_dice_roll() {
		return train_outcomes_dice_roll;
	}

	public float[] getWeightsToUseForTraining() {
		return weightsToUseForTraining;
	}
	
	// as opposed to the other getters/setters, these two work index-based
	public Float getWeightsBeingTrained(int index) {
		return weightsBeingTrained.get(index);
	}

	public void setWeightsBeingTrained(int index, Float value) {
		this.weightsBeingTrained.set(index, value);
	}
}