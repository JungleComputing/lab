package dach;

import ibis.dfs.DFSClient;
import ibis.dfs.FileInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class DistributedJobs implements JobProducer {

	private static final int LISTING_THREAD_COUNT = 10;
	
	private static final Logger logger = Logger.getLogger("dach.distributedJobs");

	private final LinkedList<String> servers = new LinkedList<String>();

	private final Problem problem;
	
	private final LinkedList<DACHJob> jobs = new LinkedList<DACHJob>();
	
	private final DFSClient client;
	
	private class ListingThread implements Runnable { 
		
		public void run() { 
		
			String server = getServer();
			
			while (server != null) { 
				List<FileInfo> tmp = produceJobs(server, problem);
				merge(tmp);
				
				server = getServer();	
			}
			
			done();
		}
	}
	
	private LinkedList<ListingThread> listingThreads = new LinkedList<ListingThread>();
	
	private HashMap<String, FileInfo> files = new HashMap<String, FileInfo>();
	
	private int done = 0; 
	
	public DistributedJobs(List<String> servers, Problem problem, 
			boolean verbose) throws Exception {
		
		this.problem = problem;		
		this.servers.addAll(servers);
		this.client = new DFSClient("master", "master");		
		
		for (int i=0;i<LISTING_THREAD_COUNT;i++) { 
			ListingThread t = new ListingThread();
			new Thread(t).start();
			listingThreads.add(t);
		}	
	}
	
	private synchronized String getServer() { 
		
		if (servers.size() == 0) { 
			return null;
		}
		
		return servers.removeFirst();
		
	}
		
	private synchronized void done() { 
		done++;
	
		if (done == LISTING_THREAD_COUNT) { 
			notifyAll();
		}
	}
	
	private synchronized void waitUntilDone() { 
	
		while (done < LISTING_THREAD_COUNT) { 
			try { 
				wait();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	private synchronized void merge(List<FileInfo> tmp) { 
		
		for (FileInfo f : tmp) {			
			FileInfo old = files.get(f.name);
			
			if (old != null) { 
				logger.info("Adding replica(s) to existing file " + f.name + " (" + f.size + "): " + f.replicaHosts);
				
				if (old.size != f.size) { 
					logger.warn("File size mismatch " + old.size + " != " + f.size);						
				}
				
				old.addReplicaHosts(f.replicaHosts);
			} else { 
				logger.info("Adding new file " + f.name + " (" + f.size + "): " + f.replicaHosts);
				files.put(f.name, f);
			}
		}
	}
	
	private List<FileInfo> produceJobs(String server, Problem problem) { 
		
		String dir = problem.directory;
		List<FileInfo> result = null;
		
		while (result == null) { 
	
			try {
				result = client.list(server, dir, true);
				logger.info(server + " returned listing of " + result.size() + " files");
				break;
			} catch (Exception e) {
				logger.warn("Failed to list files on " + server, e);
			}
	
			// Randomize the node list
		/*	
			LinkedList<String> nodes = new LinkedList<String>(c.nodes);
				
			if (!nodes.contains(c.master)) { 
				nodes.add(c.master);
			}
				
			Collections.shuffle(nodes);

			for (String s : nodes) {
				try {
					result = client.list(s, dir, true);
					logger.info(s + " returned listing of " + result.size() + " files");
					break;
				} catch (Exception e) {
					logger.warn("Failed to list files on " + s, e);
				}
			}
		*/	
			if (result == null) { 
				try { 
					Thread.sleep(2000);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}

		return result;
	}
	
	/*
	private void merge(HashMap<String, FileInfo> map, List<FileInfo> b) { 
		
		for (FileInfo f : b) {			
			FileInfo old = map.get(f.name);
			
			if (old != null) { 
				logger.info("Adding replica(s) to existing file " + f.name + " (" + f.size + "): " + f.replicaHosts);
				
				if (old.size != f.size) { 
					logger.warn("File size mismatch " + old.size + " != " + f.size);						
				}
				
				old.addReplicaHosts(f.replicaHosts);
			} else { 
				logger.info("Adding new file " + f.name + " (" + f.size + "): " + f.replicaHosts);
				map.put(f.name, f);
			}
		}
	}*/
	
	private Set<String> getFileLocations(FileInfo info) { 
		
		return info.replicaHosts;
		
		/*
		Set<String> result = new HashSet<String>();
		
		for (String host : info.replicaHosts) { 
			for (Cluster c : frontends) { 	
				if (c.isNode(host)) { 
					result.add(c.name);
				}
			}
			
			for (Cluster c : workers) { 	
				if (c.isNode(host)) { 
					result.add(c.name);
				}
			}	

		}
		
		return result;*/
	}
	
	private void createJobs(HashMap<String, FileInfo> files, Problem problem) { 
		
		LinkedList<String> keys = new LinkedList<String>(files.keySet());
		
		for (String s : keys) { 
			if (s.endsWith("t0.fits")) { 
				String other = s.substring(0, s.length()-7) + "t1.fits";
				
				FileInfo t0 = files.remove(s);
				FileInfo t1 = files.remove(other);
				
				if (t0 == null || t1 == null) { 
					logger.warn("Problem matching pair " + s + " (" + t0 + ") and " + other + " (" + t1 + ")");
				} else { 
					
					Set<String> locations = getFileLocations(t0);
					locations.addAll(getFileLocations(t1));
					
					jobs.add(new DACHJob(problem.ID, problem.directory, t0, t1, locations));					
				}
			}
		}
		
		if (!files.isEmpty()) { 
			logger.warn("Files leftover after pairing: " + files);
		}
	}
	
	/*
	private void produceJobs(Problem problem) { 
	
		HashMap<String, FileInfo> files = new HashMap<String, FileInfo>();
		 
		for (String server: servers) {
			List<FileInfo> tmp = produceJobs(server, problem);			
			merge(files, tmp);			
		}		
				
		logger.info("Total files listed: " + files.size());
		createJobs(files, problem);
	}*/
		
	
	public List<DACHJob> produceJobs(boolean skipErrors) throws IOException {

		/*
		for (Problem p : problems) { 
			produceJobs(p);
		}*/
		
		waitUntilDone();
		
		createJobs(files, problem);
				
		return jobs;
	}
/*
	private static final Logger logger = Logger.getLogger("dach.distributedJobs");
	
	private static final int DEFAULT_LIST_THREADS = 12;
	
	private final boolean verbose;

	private final String directory;
	
	private final List<Set<String>> sites;
	
	private final Collection<Problem> problems;
	
	private final HashMap<String, DACHJob> jobs = new HashMap<String, DACHJob>();

	private final List<ListTask> tasks = Collections.synchronizedList(new LinkedList<ListTask>());
	
	private class ListTask { 

		private class Tuple { 
			final FileInfo before;
			final FileInfo after;
		
			Tuple(FileInfo before, FileInfo after) { 
				this.before = before;
				this.after = after;
			}
		}

		final HashMap<String, FileInfo> single = new HashMap<String, FileInfo>();
		final LinkedList<Tuple> pairs = new LinkedList<Tuple>();
		
		final Problem p;
		final Set<String> site;
		final boolean skipErrors;
		
		ListTask(Problem p, Set<String> site, boolean skipErrors) { 
			this.p = p;
			this.site = site;
			this.skipErrors = skipErrors;
		}

		void addFile(String fullName, long length, Set<String> hosts) { 

			if (verbose) { 
				logger.info("Adding file: " + fullName);
			}


			String name = fullName.substring(0, name.length()-5); 

			if (!(name.endsWith("t0") || name.endsWith("t1"))) { 
				logger.warn("Cannot handle file: " + fullName);
				return;
			}
			
			String pre = name.substring(0, name.length()-2);
			
			// We have found new problem file
			FileInfo info = new FileInfo(fullName, length);
			info.addReplicaHosts(hosts);
			
			if (verbose) { 
				logger.info("File did not exist yet!");
			}	
			
			if (name.endsWith("t0")) { 

				String other = name.substring(0, name.length()-2) + "t1";
				
				FileInfo tmp = single.remove(other);
				
				if (tmp != null) {
					pairs.add(new Tuple(info, tmp));
					// jobs.put(pre, new DACHJob(ID, problem, info, tmp)); 
				} else { 
					single.put(name, info);
				}

			} else if (name.endsWith("t1")) { 

				String other = name.substring(0, name.length()-2) + "t0";

				FileInfo tmp = single.remove(other);

				if (tmp != null) { 
					pairs.add(new Tuple(tmp, info));
					// jobs.put(pre, new DACHJob(ID, problem, tmp, info)); 
				} else { 
					single.put(name, info);
				}
			} 

		}
		
		void getJob() throws IOException {
			
			String ID = p.ID;
			String problem = p.directory;
			
			String dir = directory + File.separator + problem;
			
			File [] files = getFiles(site, dir);
				
			for (File f : files) { 

				if (f.getName().endsWith(".fits")) { 
					addFile(ID, problem, f, site);
				} else {
					logger.warn("Skipping: " + f);
				}
			}
		
			if (verbose && single.size() > 0) { 
				logger.warn("Unpaired files: ");

				for (String k : single.keySet()) { 
					logger.warn("    " + k);
				}

				single.clear();
			}
		}
	}
	
	private class FileListingThread extends Thread {  
	
		public FileListingThread() { 
			setDaemon(true);
		}
		
		public void start() {
	
			while (true) { 

				ListTask task = null;
				
				synchronized (tasks) {
					while (tasks.size() == 0) { 
						try { 
							wait();
						} catch (InterruptedException e) {
							// ignore
						}
					}

					if (tasks.size() > 0) { 
						task = tasks.remove(0); 
					}
				}

				if (task != null) { 
					try {
						task.getJob();
					} catch (IOException e) {
						logger.warn("Failed to read problem set " + task.p.ID, e);
						
						if (!task.skipErrors) { 
							System.exit(1);
						}
					}
				}
			}		
		}
	}
	
	public DistributedJobs(String directory, List<Set<String>> sites, Collection<Problem> problems, boolean verbose) { 
		this.directory = directory;
		this.problems = problems;
		this.sites = sites;
		this.verbose = verbose;
		
		for (int i=0;i<DEFAULT_LIST_THREADS;i++) { 
			new FileListingThread().start();
		}
	}
	
	private void addFile(String ID, String problem, String fullName, long length, Set<String> hosts) { 

		String name = fullName.substring(0, name.length()-5); 

		if (!(name.endsWith("t0") || name.endsWith("t1"))) { 
			logger.warn("Cannot handle file: " + fullName);
			return;
		}
		
		String pre = name.substring(0, name.length()-2);
		
		DACHJob job = jobs.get(pre);
		
		if (job != null) { 
			
			logger.info("Adding replica");
			
			// We have found a file replica!
			if (name.endsWith("t0")) { 
				job.beforeInfo.addReplicaHosts(hosts);
				logger.info(name + " replicas: " + job.beforeInfo.replicaHosts + " (" + job.beforeInfo.replicaSites + ")");
			} else {
				job.afterInfo.addReplicaHosts(hosts);

				logger.info(name + " replicas: " + job.afterInfo.replicaHosts + " (" + job.afterInfo.replicaSites + ")");
			}
			
		} else { 

			if (verbose) { 
				logger.info("Adding file: " + fullName);
			}
			
			// We have found new problem file
			FileInfo info = new FileInfo(fullName, length);
			info.addReplicaHosts(hosts);
			
			if (verbose) { 
				logger.info("File did not exist yet!");
			}	
			
			if (name.endsWith("t0")) { 

				String other = name.substring(0, name.length()-2) + "t1";
				
				FileInfo tmp = single.remove(other);
				
				if (tmp != null) {
					jobs.put(pre, new DACHJob(ID, problem, info, tmp)); 
				} else { 
					single.put(name, info);
				}

			} else if (name.endsWith("t1")) { 

				String other = name.substring(0, name.length()-2) + "t0";

				FileInfo tmp = single.remove(other);

				if (tmp != null) { 
					jobs.put(pre, new DACHJob(ID, problem, tmp, info)); 
				} else { 
					single.put(name, info);
				}
			} 
		}
	}
	
	private File [] getFiles(String host, String dir) throws IOException { 

		try {
 			File tmp = GAT.createFile(new URI("any://" + host + "/" + dir));
 			
 			File [] res = tmp.listFiles();
 			
 			logger.info("Files in any://" + host + dir + " : " + (res != null ? Arrays.toString(res) : "null")); 
 			
 			return tmp.listFiles();
    	} catch (Exception e) {
			throw new IOException("Failed to list any://" + host + "/" + dir);
    	}
	}
	
	private File [] getFiles(Set<String> hosts, String dir) throws IOException { 

		if (hosts == null || hosts.size() == 0) { 
			throw new IOException("No hosts specified");
		}
		
		for (String host : hosts) { 
			try { 
				return getFiles(host, dir);
			} catch (IOException e) { 
				logger.warn("Failed to list files on " + hosts, e);
			}
		}

		throw new IOException("Failed to list files on any host!");
	}
	
	private void getJob(Problem p) throws IOException {
		
		String ID = p.ID;
		String problem = p.directory;
		
		String dir = directory + File.separator + problem;
		
		for (Set<String> site : sites) { 
			
			File [] files = getFiles(site, dir);
			
			for (File f : files) { 

				if (f.getName().endsWith(".fits")) { 
					addFile(ID, problem, f, site);
				} else {
					logger.warn("Skipping: " + f);
				}
			}
			
			if (verbose && single.size() > 0) { 
				logger.warn("Unpaired files: ");

				for (String k : single.keySet()) { 
					logger.warn("    " + k);
				}

				single.clear();
			}
		}
	}
	
	public List<DACHJob> produceJobs(boolean skipErrors) throws IOException {

		for (Problem p : problems) { 
			for (Set<String> s : sites) { 
				synchronized (tasks) {
					tasks.add(new ListTask(p, s, skipErrors);
					tasks.notifyAll();
				}
			}
		}		
				
		// TODO wait for tasks!!!
		
		return new LinkedList<DACHJob>(jobs.values());
	}
*/
	

}
