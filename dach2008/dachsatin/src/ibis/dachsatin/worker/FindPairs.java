package ibis.dachsatin.worker;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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

	public FindPairs(File directory, Collection<Problem> problems, boolean verbose) { 
		this.directory = directory;
		this.problems = problems;
		this.verbose = verbose;
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
				pairs.add(new Pair(ID, problem, f.getName(), tmp.getName()));
			} else { 
				single.put(name, f);
			}

		} else if (name.endsWith("t1")) { 

			String other = name.substring(0, name.length()-2) + "t0";

			File tmp = single.remove(other);

			if (tmp != null) { 
				pairs.add(new Pair(ID, problem, tmp.getName(), f.getName()));
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

		return pairs;
	}
}


