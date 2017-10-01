package secvm_server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

public final class DataUtils {
	
	public static float computeStepSize(int iteration, float lambda) {
		return lambda / iteration;
	}
	
	public static void applySubgradientUpdate(
			List<Float> weights, int iteration, float lambda,
			AtomicIntegerArray subgradient, int numGradientUpdateVectors) {
		
		float stepSize = computeStepSize(iteration, lambda);
		
		for (int i = 0; i < weights.size(); ++i) {
			weights.set(i,
					(1 - stepSize * lambda) * weights.get(i) +
					subgradient.get(i) / (float) numGradientUpdateVectors);
		}
	}
	
	public static <T extends Number> String numberListToBase64(List<T> list) {
		// TODO: implementation
		return "";
	}
	
	public static <T extends Number> List<T> base64ToNumberList(String string) {
		// TODO: implementation
		return new ArrayList<>();
	}
	
	public static String atomicIntegerArrayToBase64(AtomicIntegerArray array) {
		// TODO: implementation
		return "";
	}
	
	public static AtomicIntegerArray base64ToAtomicIntegerArray(String string) {
		// TODO: implementation
		return new AtomicIntegerArray(0);
	}
	
	public static void addToFirstVector(List<Float> l1, List<Float> l2) {
		for (int i = 0; i < l1.size(); ++i) {
			l1.set(i, l1.get(i) + l2.get(i));
		}
	}
	
	public static void divideVector(List<Float> list, float divisor) {
		for (int i = 0; i < list.size(); ++i) {
			list.set(i, list.get(i) / divisor);
		}
	}

}
