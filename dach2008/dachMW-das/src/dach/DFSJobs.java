package dach;

import ibis.dfs.FileInfo;
import ibis.util.RunProcess;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Utility class that finds file pair in a given directory. 
 * 
 * @author Jason Maassen
 *
 */
public class DFSJobs implements JobProducer {

	private final boolean verbose;

	private final File directory;
	
	private final Problem problem;

	private final HashMap<String, File> single = new HashMap<String, File>();

	private final LinkedList<DACHJob> jobs = new LinkedList<DACHJob>();

	private String [] locationCommand = { "/data/local/gfarm_v2/bin/gfwhere", "" };
	
	public DFSJobs(File directory, Problem problem, String locationCMD, boolean verbose) { 
		this.directory = directory;
		this.problem = problem;
		this.verbose = verbose;
	
		if (locationCMD != null) { 
			locationCommand[0] = locationCMD;
		}
	}

	private void addFile(String ID, String problem, File f) { 

		String name = f.getName();

		if (verbose) { 
			System.out.println("Adding file: " + f.getName());
		}

		name = name.substring(0, name.length()-5); 

		if (name.endsWith("t0")) { 

			String other = name.substring(0, name.length()-2) + "t1";

			File tmp = single.remove(other);

			if (tmp != null) { 
				jobs.add(new DACHJob(ID, problem, new FileInfo(f.getName(), f.length()), 
						new FileInfo(tmp.getName(), tmp.length()), null));
			} else { 
				single.put(name, f);
			}

		} else if (name.endsWith("t1")) { 

			String other = name.substring(0, name.length()-2) + "t0";

			File tmp = single.remove(other);

			if (tmp != null) { 
				jobs.add(new DACHJob(ID, problem, new FileInfo(tmp.getName(), tmp.length()), 
						new FileInfo(f.getName(), f.length()), null));
			} else { 
				single.put(name, f);
			}
		} else { 
			if (verbose) { 
				System.out.println("Cannot handle file: " + f);
			}
		}
	}

	private void getJob(Problem p) throws IOException {
		
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
		}
	}
	
	private Set<String> getReplicaHosts(String file) { 
		
		locationCommand[1] = file;

		RunProcess p = new RunProcess(locationCommand);
		p.run();
		int result = p.getExitStatus();

		if (result != 0) { 
			System.err.println("FAILED to get replication sites for file " + file
					+ ": " + new String(p.getStderr()));
			return null;
		}

		String out = new String(p.getStdout()).trim();

		if (out.length() == 0) { 
			System.err.println("FAILED to get replication sites for file " + file
					+ ": NO OUTPUT");
			return null;
		}

		Set<String> tmp = new HashSet<String>();

		StringTokenizer t = new StringTokenizer(out);

		while (t.hasMoreTokens()) { 
			tmp.add(t.nextToken());
		}

		return tmp;
	}
	
	private void getReplicaLocations(DACHJob p) { 
		p.beforeInfo.addReplicaHosts(getReplicaHosts(p.getBefore()));
		p.afterInfo.addReplicaHosts(getReplicaHosts(p.getAfter()));
	}
		
	public List<DACHJob> produceJobs(boolean skipErrors, int duplicate) throws IOException { 

		//for (Problem p : problems) { 
			
			if (skipErrors) {
				try { 
					getJob(problem);
				} catch (Exception e) {
					if (verbose) { 
						System.out.println("Failed to read problem set " + problem);
						e.printStackTrace();
					}
				}
			} else { 
				getJob(problem);
			}				
		//}

		for (DACHJob p : jobs) { 
			getReplicaLocations(p);
		}
		
		return jobs;
	}
}


