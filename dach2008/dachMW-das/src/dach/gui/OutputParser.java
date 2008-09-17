package dach.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class OutputParser {
	
	private static final String STEAL_REPLY = "Got steal reply";
	private static final String STEAL_REQUEST = "Sending steal request";
	
	long totalComputeTime = 0;
	long totalWorkTime = 0;
	
	private long nodeComputeTime = 0;
	private long nodeWorkTime = 0;
	
	private boolean old = false;
	
	private long startTime;
	
	public OutputParser(String startTime, boolean old) { 
		this.old = old;
		
		this.startTime = getTime(startTime);
	}
	
	public long getTime(String time) { 

		StringTokenizer tok = new StringTokenizer(time, ":");
		
		if (tok.countTokens() != 3) { 
			System.err.println("Cannot parse time! " + time);
			return 0;
		}
		
		long hour = Integer.parseInt(tok.nextToken());
		long minute = Integer.parseInt(tok.nextToken());
		long second = Integer.parseInt(tok.nextToken());
		
		long t = second + 60*minute + 60*60*hour;
		
		if (t < startTime) { 
			t += 24*60*60;
		}
		
		return t;
	}
	
	private void skipTokens(StringTokenizer tok, int num) { 
		for (int i=0;i<num;i++) { 
			tok.nextToken();
		}
	}
	
	private void parseLine(NodeStatistics s, long startTime, String line) { 
		
		if (line == null) { 
			return;
		}
		
		line = line.trim();
		
		if (line.length() == 0) { 
			return;
		}
		
		StringTokenizer tok = new StringTokenizer(line);
		
		if (tok.countTokens() < 2) {
			System.err.println("Failed to parse: " + line);
			return;
		}
		
		long time = getTime(tok.nextToken()) - startTime;
		
		if (line.contains(STEAL_REPLY)) { 
			
			if (line.contains("(no work,")) { 
				s.addEvent(NodeStatistics.EventType.STEAL_REPLY_EMPTY, -1, time, null);
			} else if (line.contains("(done,")) { 
				s.addEvent(NodeStatistics.EventType.STEAL_REPLY_DONE, -1, time, null);
			} else if (line.contains("(work,")) { 
			
				skipTokens(tok, 11);
				
				long jobID = Long.parseLong(tok.nextToken());
				
				s.addEvent(NodeStatistics.EventType.STEAL_REPLY_FULL, jobID, time, null);
			}
			return;
			
		} else if (line.endsWith(STEAL_REQUEST)) { 
			s.addEvent(NodeStatistics.EventType.STEAL_REQUEST, -1, time, null);
			return;
		} 

		if (old) { 
			if (tok.countTokens() < 5) {
				System.err.println("Failed to parse: " + line);
				return;
			}

			for (int i=0;i<4;i++) { 
				tok.nextToken();
			}

			String next = tok.nextToken();

			if (next.equals("Completed")) { 

				for (int i=0;i<4;i++) { 
					tok.nextToken();
				}

				long computeTime = Long.parseLong(tok.nextToken());
				long compute = time - (computeTime / 1000);

				totalComputeTime += computeTime;
				nodeComputeTime += computeTime;

				for (int i=0;i<3;i++) { 
					tok.nextToken();
				}

				long total = Long.parseLong(tok.nextToken());

				totalWorkTime += total;
				nodeWorkTime += total;

				s.addEvent(NodeStatistics.EventType.START_COMPUTE, -1, compute, null);
				s.addEvent(NodeStatistics.EventType.JOB_RESULT, -1, time, null);
				return;			
			} else { 
				System.err.println("Failed to parse: " + line);
			}
		} else { 
			
			if (line.contains("Completed")) { 

				skipTokens(tok, 6);
				
				long jobID = Long.parseLong(tok.nextToken());
				
				// Skip next 2/3 tokens
				
				// NOTE: change from 3 to 2 for format 2	
				skipTokens(tok, 3);
				
				long [] data = new long[5];
				
				// Total time
				data[0] = Long.parseLong(tok.nextToken());
				
				// Skip next token
				skipTokens(tok, 1);
				
				// Total transfer time and 2 individual times
				data[1] = Long.parseLong(tok.nextToken());
				data[2] = Long.parseLong(tok.nextToken());
				data[3] = Long.parseLong(tok.nextToken());

				skipTokens(tok, 1);
				
				// Compute time
				data[4] = Long.parseLong(tok.nextToken());
				
				totalComputeTime += data[4];
				nodeComputeTime += data[4];
				
				totalWorkTime += data[0];
				nodeWorkTime += data[0];
				
				s.addEvent(NodeStatistics.EventType.JOB_RESULT, jobID, time, data);
				
				return;			
			}
			
		}
		
	}
	
	public NodeStatistics parseNode(String cluster, String node, long startTime, File f) throws IOException { 
		
		nodeComputeTime = 0;
		nodeWorkTime = 0;
		
		BufferedReader r = new BufferedReader(new FileReader(f));
		
		NodeStatistics s = new NodeStatistics(cluster, node);
		
		try { 
			String line = r.readLine();
		
			while (line != null) { 
				parseLine(s, startTime, line);
				line = r.readLine();
			}
		} finally {  
			r.close();
		}
		
		System.err.println("Node time: " + nodeWorkTime + " " + nodeComputeTime + " " + (nodeWorkTime-nodeComputeTime));
		
		return s;
	}
	
	public ClusterStatistics parseCluster(String cluster, int cores, boolean runMultiCore, 
			long startTime, String [] nodes, File [] files) throws IOException { 

		ClusterStatistics c = new ClusterStatistics(cluster, cores, runMultiCore);
		
		for (int i=0;i<nodes.length;i++) { 
			NodeStatistics n = parseNode(cluster, nodes[i], startTime, files[i]);
			c.addNode(n);
		}

		return c;
	}

	public LinkedList<ClusterStatistics> parseAll(File directory, 
			long startTime, String prefix, String [] clusters, 
			String postfix) throws IOException { 
		
		totalWorkTime = 0;
		totalComputeTime = 0;
		
		LinkedList<ClusterStatistics> result = new LinkedList<ClusterStatistics>();
		
		File [] listing = directory.listFiles();
		
		LinkedList<String> nodes = new LinkedList<String>();
		LinkedList<File> files = new LinkedList<File>();
		
		for (String cluster : clusters) { 
			
			StringTokenizer tok = new StringTokenizer(cluster, ":");
			
			if (tok.countTokens() != 3) { 
				System.err.println("Failed to parse cluster info: " + cluster);
				System.exit(1);
			}
			
			String clustername = tok.nextToken();
			int cores = Integer.parseInt(tok.nextToken());
			boolean runMultiCore = tok.nextToken().equalsIgnoreCase("M");
			
			String tmp = prefix + clustername;
			
			for (int i=0;i<listing.length;i++) { 
		
				File f = listing[i];
				
				if (f != null) { 

					String name = f.getName();

					if (name.startsWith(tmp) && name.endsWith(postfix)) { 
						String node = name.substring(tmp.length(), name.length()-postfix.length());
						nodes.addLast(node);
						files.addLast(f);
						listing[i] = null;
					}
				}
			}

			if (nodes.size() > 0) { 
				ClusterStatistics c = parseCluster(clustername, 
						cores, runMultiCore, startTime, 
						nodes.toArray(new String[nodes.size()]), 
						files.toArray(new File[files.size()]));
				
				c.sort();
				
				result.addLast(c);
				
				nodes.clear();
				files.clear();	
			}
		}
		
		return result;		
	}
	
public static void main(String [] args) { 
		
		if (args.length < 5) { 
			System.err.println("Usage: OutputParser.main time dir prefix postfix cluster+");
			System.exit(1);
		}
		
		OutputParser p = new OutputParser(args[0], false);
		
		long startTime = p.getTime(args[0]); 
		File dir = new File(args[1]);
		String prefix = args[2];
		String postfix = args[3];
		
		String [] clusters = new String[args.length-4];
		System.arraycopy(args, 4, clusters, 0, args.length-4);
		
		
		
		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.err.println("Got " + s.size() + " clusters");
		
			long endTime = 0;
			long jobEndTime = 0;
			
			for (ClusterStatistics c : s) { 
				System.err.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");
			
				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				endTime = Math.max(endTime, c.getLatestEndTime());
			}
		
			System.err.println("end time = " + endTime);
			System.err.println("job end time = " + jobEndTime);
	
			System.err.println("total work time = " + p.totalWorkTime);
			System.err.println("total compute time = " + p.totalComputeTime);
			System.err.println("total transfer time = " + (p.totalWorkTime - p.totalComputeTime));

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

