package ibis.dachsatin.worker;



import ibis.util.RunProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Utility class that finds file pair in a given directory. 
 * 
 * @author Jason Maassen
 *
 */
public class FindPairs {

	private final boolean verbose;

	private final File directory;
	
	private final Collection<Problem> problems;

	private final HashMap<String, File> single = new HashMap<String, File>();

	private final ArrayList<Pair> pairs = new ArrayList<Pair>();

	private String [] locationCommand = { "/data/local/gfarm_v2/bin/gfwhere", "" };
	
	public FindPairs(File directory, Collection<Problem> problems, String locationCMD, boolean verbose) { 
		this.directory = directory;
		this.problems = problems;
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
				pairs.add(new Pair(ID, problem, f.getName(), f.length(), tmp.getName(), tmp.length()));
			} else { 
				single.put(name, f);
			}

		} else if (name.endsWith("t1")) { 

			String other = name.substring(0, name.length()-2) + "t0";

			File tmp = single.remove(other);

			if (tmp != null) { 
				pairs.add(new Pair(ID, problem, tmp.getName(), tmp.length(), f.getName(), f.length()));
			} else { 
				single.put(name, f);
			}
		} else { 
			if (verbose) { 
				System.out.println("Cannot handle file: " + f);
			}
		}
	}

	private void getPairs(Problem p) throws IOException {
		
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
	
	private String getDomain(String host) { 
		
		if (host == null) { 
			return null;
		}
		
		host = host.trim();
		
		int index = host.indexOf('.');
		
		if (index <= 0) { 
			return null;
		}
	
		return host.substring(index+1);
	}
	
	/*
	private Set<String> getReplicaLocations(String file) { 
		
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
			String domain = getDomain(t.nextToken());

			if (domain != null) { 
				tmp.add(domain);
			}
		}

		return tmp;
	}*/

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
	
	public void getReplicaLocations(Pair p) { 
		p.before.addReplicaHosts(getReplicaHosts(p.getBefore()));
		p.after.addReplicaHosts(getReplicaHosts(p.getAfter()));
	}
		
	public ArrayList<Pair> getPairs(boolean skipErrors) throws IOException { 

		for (Problem p : problems) { 
			
			if (skipErrors) {
				try { 
					getPairs(p);
				} catch (Exception e) {
					if (verbose) { 
						System.out.println("Failed to read problem set " + problems);
						e.printStackTrace();
					}
				}
			} else { 
				getPairs(p);
			}				
		}

		for (Pair p : pairs) { 
			getReplicaLocations(p);
		}
		
		return pairs;
	}
}


