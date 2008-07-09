package ibis.dachsatin;

import ibis.util.Pair;

import java.io.Serializable;
import java.util.Arrays;

public class Result implements Serializable {

	private static final long serialVersionUID = -1661420366726877489L;
	
	public final Pair [] input;	
	public final String [] output;
	public final long [] time;
	
	public Result(Pair [] input, String output, long time) {
		this.input = input;
		this.output = new String [] { output };
		this.time = new long [] { time };
	}	
	
	public Result(Result resA, Result resB) {		
		input = Arrays.copyOf(resA.input, resA.input.length + resB.input.length);
		System.arraycopy(resB.input, 0, input, resA.input.length, resB.input.length);
		
		output = Arrays.copyOf(resA.output, resA.output.length + resB.output.length);
		System.arraycopy(resB.output, 0, output, resA.output.length, resB.output.length);
		
		time = Arrays.copyOf(resA.time, resA.time.length + resB.time.length);
		System.arraycopy(resB.time, 0, time, resA.time.length, resB.time.length);
	}	
	
	public String mergeOutput() {
		
		StringBuilder sb = new StringBuilder();
		
		for (String s : output) { 
			sb.append(s);			
		}
		
		return sb.toString();
	}
	
	public long totalTime() {
		
		long total = 0;
		
		for (int i=0;i<time.length;i++) { 
			total += time[i];			
		}
		
		return total;
	}
	
	public long avgTime() {
		return totalTime() / time.length;
	}
	
}
