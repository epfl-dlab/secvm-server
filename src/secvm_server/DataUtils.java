package secvm_server;

import java.util.ArrayList;
import java.util.List;

public final class DataUtils {
	
	public static <T extends Number> String numberListToBase64(List<T> list) {
		// TODO: implementation
		return "";
	}
	
	public static <T extends Number> List<T> base64ToNumberList(String string) {
		// TODO: implementation
		return new ArrayList<>();
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
