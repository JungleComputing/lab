package ibis.dachsatin;


import java.io.Serializable;
import java.util.List;

public class Result implements Serializable {

	private static final long serialVersionUID = -1661420366726877489L;
	
	public final Pair input;
	public final String machineID;	
	
	public String stdout;
	public String stderr;
	public String result;
	
	public long time;
	public long transferTime;
	
	public boolean failed = false;
	
	private transient long startTime;
	
	public Result(Pair input, String machineID) {
		this.input = input;
		this.machineID = machineID;
		
		this.stdout = "";
		this.stderr = "";
		this.result = "";		
		
		startTime = System.currentTimeMillis();
	}	
	
	public void addTransferTime(long time) { 
		transferTime += time;
	}
	
	private long time() { 
		return System.currentTimeMillis() - startTime;
	}
	
	public void info(String text, long time) {
		stdout += time + ", " + time() + ": " + text;
		
		System.out.println("INFO( " + time + ", " + time() + " ): " + text);
	}

	public void fatal(String text, long time) { 
		stderr += time + " , " + time() + ": " + text;
		failed = true;
		
		System.out.println("FATAL( " + time + " , " + time() + " ): " + text);
	}
	
	public void error(String text, long time) { 
		stderr += time + ", " + time() + ": " + text;

		System.out.println("ERROR( " + time + " , " + time() + " ): " + text);
	}

	public void setResult(String text, long t) { 
		result = text;		
		time = System.currentTimeMillis() - startTime;

		System.out.println("RESULT( " + t + " , " + time() + " ): finished job " + input.before + " - " 
				+ input.after + " in " + time + " ms.");
	}
	
	public static String mergeResult(List<Result> results) {
		
		StringBuilder sb = new StringBuilder();
		
		for (Result r : results) { 
			sb.append(r.result);			
		}
		
		return sb.toString();
	}
	
	public static long totalTime(List<Result> results) {
		
		long total = 0;
		
		for (Result r : results) { 
			total += r.time;			
		}
		
		return total;
	}
	
	public static long averageTime(List<Result> results) {
		return totalTime(results) / results.size();
	}	
}
