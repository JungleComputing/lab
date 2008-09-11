package dach;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LocalJobs implements JobProducer {

	private final boolean verbose;

	private final File directory;
	
	private final List<String> sites;
	
	private final Collection<Problem> problems;

	private final HashMap<String, File> single = new HashMap<String, File>();

	private final LinkedList<DACHJob> jobs = new LinkedList<DACHJob>();
	
	public LocalJobs(File directory, List<String> sites, Collection<Problem> problems, boolean verbose) { 
		this.directory = directory;
		this.problems = problems;
		this.sites = sites;
		this.verbose = verbose;
	}
	
	private void getJob(Problem p) throws IOException {
	/*	
		String ID = p.ID;
		String problem = p.directory;
		
		File dir = new File(directory + File.separator + problem);
		
		if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) { 
			throw new IOException("Directory " + dir + " does not exist");
		}	
		
		File [] files = dir.listFiles();

		for (File f : files) { 

			if (f.getName().endsWith(".fits")) { 
				addFile(ID, problem, f);
			} else {
				if (verbose) { 
					System.out.println("ERROR: Skipping: " + f);
				}
			}
		}

		if (verbose && single.size() > 0) { 
			System.out.println("ERROR: Unpaired files:");

			for (String k : single.keySet()) { 
				System.out.println("ERROR:    " + k);
			}

			single.clear();
		}*/
	}
	
	public List<DACHJob> produceJobs(boolean skipErrors) throws IOException {

		for (Problem p : problems) { 
			
			if (skipErrors) {
				try { 
					getJob(p);
				} catch (Exception e) {
					if (verbose) { 
						System.out.println("Failed to read problem set " + problems);
						e.printStackTrace();
					}
				}
			} else { 
				getJob(p);
			}				
		}

		return jobs;
	}
}
