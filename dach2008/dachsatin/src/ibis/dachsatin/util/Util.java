package ibis.dachsatin.util;

public class Util {

	public static final int MAX_ATTEMPTS = 10;

	// Timing hack
	public static long start = -1;

	// Local directory hack
	public static String dataDir = null;

	// Local temp dir hack
	public static String tmpDir = null;

	// Local copy operation hack
	public static String cpExec = null;

	// Machine identification hack
	public static String machineID = null;

	// File locality hack
	public static String domain = null;
	
	// Local executable hack
	public static String exec = null;
	
	// Location command hack
	public static String location = null;
	
	// Static block to initialize local static configuration.
	static { 

		start = System.currentTimeMillis();

		dataDir = System.getProperty("dach.dir.data");

		if (dataDir == null) { 
			System.err.println("DACH data dir not set! (dach.dir.data)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		if (!FileUtils.directoryExists(dataDir)) {
			System.err.println("DACH data dir (" + dataDir + ") not found!");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		tmpDir = System.getProperty("dach.dir.tmp");

		if (tmpDir == null) {  
			System.err.println("DACH tmp dir not set! (dach.dir.tmp)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		if (!FileUtils.directoryExists(tmpDir)) {
			System.err.println("DACH tmp dir (" + tmpDir + ") not found!");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}


		machineID = System.getProperty("dach.machine.id");

		if (machineID == null) { 
			System.err.println("DACH machine ID not set! (dach.machine.id)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		domain = System.getProperty("dach.domain");

		if (domain == null) { 
			System.err.println("DACH domain not set! (dach.domain)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}
		
		exec = System.getProperty("dach.executable");

		if (exec == null ) {
			System.err.println("DACH executable not set! (dach.executable)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		if (!FileUtils.fileExists(exec)) {
			System.err.println("DACH executable (" + exec + ") not found!");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		cpExec = System.getProperty("dach.copy");

		if (cpExec == null ) {
			System.err.println("DACH copy executable not set! (dach.copy)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		if (!FileUtils.fileExists(cpExec)) {
			System.err.println("DACH copy executable (" + cpExec + ") not found!");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}
		
		location = System.getProperty("dach.location");

		if (location == null ) {
			System.err.println("DACH location executable not set! (dach.location)");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}

		if (!FileUtils.fileExists(location)) {
			System.err.println("DACH location executable (" + location + ") not found!");
			// NOTE: this is fatal! Commit suicide to prevent stealing any additional jobs!  
			System.exit(1);
		}
	}

	public static long time() { 
		return System.currentTimeMillis() - start;
	}

	public static void check() {
		// dummy used to trigger the static block ?
	}
	
}
