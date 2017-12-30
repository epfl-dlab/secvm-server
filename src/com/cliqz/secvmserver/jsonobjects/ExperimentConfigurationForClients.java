package com.cliqz.secvmserver.jsonobjects;

// TODO: Maybe write a custom JsonSerializer to exclude hosts or titleWords from Features
// if they are not used.
// http://memorynotfound.com/dynamically-exclude-json-fields-with-gson-from-object/

/**
 * Contains the data that is put into the configuration file for the clients.
 */
public class ExperimentConfigurationForClients {
	public ExperimentId[] experimentId;
	public Features[] features;
	public String[] featuresToDelete;
	public DiceRolls[] diceRolls;
	public String[] weightVectorUrl;
	public int[] timeLeft;
	
	
	public static class ExperimentId {
		public int svmId;
		public int iteration;
		public ExperimentId(int svmId, int iteration) {
			this.svmId = svmId;
			this.iteration = iteration;
		}
	}
	
	public static class Features {
		public String idHosts;
		public int numHosts;
		public int numHashesHosts;
		public String idTitleWords;
		public int numTitleWords;
		public int numHashesTitleWords;
	}
	
	public static class DiceRolls {
		public String id;
		public float[] probs;
		public int[] train;
		public int[] test;
		public DiceRolls(String id, float[] probs, int[] train, int[] test) {
			this.id = id;
			this.probs = probs;
			this.train = train;
			this.test = test;
		}
	}
}
