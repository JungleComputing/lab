package dach;

import java.io.IOException;
import java.util.List;

public interface JobProducer {
	public List<DACHJob> produceJobs(boolean skipErrors, int duplicate) throws IOException; 	
}
