package secvm_server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
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
	
	// TODO: The Base64 encoding isn't the same for the client and the server. Fix this.
	public static String integerListToBase64(List<Integer> list) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(list.size() * Integer.BYTES);
		for (int entry : list) {
			byteBuffer.putInt(entry);
		}
		return Base64.getEncoder().encodeToString(byteBuffer.array());
	}
	
	public static List<Integer> base64ToIntegerList(String string) {
		byte[] intBytes = Base64.getDecoder().decode(string);
		ByteBuffer intByteBuffer = ByteBuffer.wrap(intBytes);
		List<Integer> intList = new ArrayList<>(intBytes.length / Integer.BYTES);
		
		while (intByteBuffer.hasRemaining()) {
			intList.add(intByteBuffer.getInt());
		}
		return intList;
	}
	
	public static String floatListToBase64(List<Float> list) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(list.size() * Integer.BYTES);
		for (float entry : list) {
			byteBuffer.putFloat(entry);
		}
		return Base64.getEncoder().encodeToString(byteBuffer.array());
	}
	
	public static List<Float> base64ToFloatList(String string) {
		byte[] floatBytes = Base64.getDecoder().decode(string);
		ByteBuffer floatByteBuffer = ByteBuffer.wrap(floatBytes);
		List<Float> floatList = new ArrayList<>(floatBytes.length / Float.BYTES);
		
		while (floatByteBuffer.hasRemaining()) {
			floatList.add(floatByteBuffer.getFloat());
		}
		return floatList;
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
