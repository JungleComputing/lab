package ibis.util;

import java.io.File;
import java.util.ArrayList;
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

	private final HashMap<String, File> single = new HashMap<String, File>();

	private final ArrayList<Pair> pairs = new ArrayList<Pair>();

	public FindPairs(File directory) { 
		this(directory, false);
	}

	public FindPairs(File directory, boolean verbose) { 
		this.directory = directory;
		this.verbose = verbose;
	}

	private void addFile(File f) { 

		String name = f.getName();

		if (verbose) { 
			System.out.println("Adding file: " + f.getName());
		}

		name = name.substring(0, name.length()-5); 

		if (name.endsWith("t0")) { 

			String other = name.substring(0, name.length()-2) + "t1";

			File tmp = single.remove(other);

			if (tmp != null) { 
				pairs.add(new Pair(f, tmp));
			} else { 
				single.put(name, f);
			}

		} else if (name.endsWith("t1")) { 

			String other = name.substring(0, name.length()-2) + "t0";

			File tmp = single.remove(other);

			if (tmp != null) { 
				pairs.add(new Pair(tmp, f));
			} else { 
				single.put(name, f);
			}
		} else { 
			if (verbose) { 
				System.out.println("Cannot handle file: " + f);
			}
		}
	}

	public Pair [] getPairs() { 

		File [] files = directory.listFiles();

		for (File f : files) { 

			if (f.getName().endsWith(".fits")) { 
				addFile(f);
			} else {
				if (verbose) { 
					System.out.println("Skipping: " + f);
				}
			}
		}

		if (verbose && single.size() > 0) { 
			System.out.println("Unpaired files:");

			for (String k : single.keySet()) { 
				System.out.println(k);
			}

			single.clear();
		}

		return pairs.toArray(new Pair[pairs.size()]);
	}
}

