package dach.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class OutputParser {
	
	private static final String EMPTY_REPLY = "Got steal reply: null false";
	private static final String STEAL_REQUEST = "Sending steal request";
	
	private long totalComputeTime = 0;
	private long totalWorkTime = 0;
	
	public long getTime(String time) { 

		StringTokenizer tok = new StringTokenizer(time, ":");
		
		if (tok.countTokens() != 3) { 
			System.out.println("Cannot parse time! " + time);
			return 0;
		}
		
		long hour = Integer.parseInt(tok.nextToken());
		long minute = Integer.parseInt(tok.nextToken());
		long second = Integer.parseInt(tok.nextToken());
		
		return second + 60*minute + 60*60*hour; 
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
			System.out.println("Failed to parse: " + line);
			return;
		}
		
		long time = getTime(tok.nextToken()) - startTime;
		
		if (line.endsWith(EMPTY_REPLY)) { 
			s.addEvent(NodeStatistics.EventType.STEAL_REPLY_EMPTY, time);
			return;
		} else if (line.endsWith(STEAL_REQUEST)) { 
			s.addEvent(NodeStatistics.EventType.STEAL_REQUEST, time);
			return;
		} 

		if (tok.countTokens() < 5) {
			System.out.println("Failed to parse: " + line);
			return;
		}

		for (int i=0;i<4;i++) { 
			tok.nextToken();
		}

		String next = tok.nextToken();
		
		if (next.equals("Got")) { 
			s.addEvent(NodeStatistics.EventType.STEAL_REPLY_FULL, time);
			return;			
		} else if (next.equals("Completed")) { 
			
			for (int i=0;i<4;i++) { 
				tok.nextToken();
			}
			
			long computeTime = Long.parseLong(tok.nextToken());
			long compute = time - (computeTime / 1000);
			
			totalComputeTime += computeTime;
			
			for (int i=0;i<3;i++) { 
				tok.nextToken();
			}
			
			long total = Long.parseLong(tok.nextToken());
		
			totalWorkTime += total;
			
			s.addEvent(NodeStatistics.EventType.START_COMPUTE, compute);
			s.addEvent(NodeStatistics.EventType.JOB_RESULT, time);
			return;			
		} else { 
			System.out.println("Failed to parse: " + line);
		}
	}
	
	public NodeStatistics parseNode(String cluster, String node, long startTime, File f) throws IOException { 
		
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
		
		return s;
	}
	
	public ClusterStatistics parseCluster(String cluster, long startTime, String [] nodes, File [] files) throws IOException { 

		ClusterStatistics c = new ClusterStatistics(cluster);
		
		for (int i=0;i<nodes.length;i++) { 
			NodeStatistics n = parseNode(cluster, nodes[i], startTime, files[i]);
			c.addNode(n);
		}

		return c;
	}

	public LinkedList<ClusterStatistics> parseAll(File directory, long startTime, String prefix, String [] clusters, String postfix) throws IOException { 
		
		totalWorkTime = 0;
		totalComputeTime = 0;
		
		LinkedList<ClusterStatistics> result = new LinkedList<ClusterStatistics>();
		
		File [] listing = directory.listFiles();
		
		LinkedList<String> nodes = new LinkedList<String>();
		LinkedList<File> files = new LinkedList<File>();
		
		for (String cluster : clusters) { 
			
			String tmp = prefix + cluster;
			
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
				ClusterStatistics c = parseCluster(cluster, startTime, 
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
		
		OutputParser p = new OutputParser();
		
		long startTime = p.getTime(args[0]); 
		File dir = new File(args[1]);
		String prefix = args[2];
		String postfix = args[3];
		
		String [] clusters = new String[args.length-4];
		System.arraycopy(args, 4, clusters, 0, args.length-4);
		
		
		
		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.out.println("Got " + s.size() + " clusters");
		
			long endTime = 0;
			long jobEndTime = 0;
			
			for (ClusterStatistics c : s) { 
				System.out.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");
			
				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				endTime = Math.max(endTime, c.getLatestEndTime());
			}
		
			System.out.println("end time = " + endTime);
			System.out.println("job end time = " + jobEndTime);
	
			System.out.println("total work time = " + p.totalWorkTime);
			System.out.println("total compute time = " + p.totalComputeTime);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

