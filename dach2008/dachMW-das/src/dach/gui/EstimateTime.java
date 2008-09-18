package dach.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class EstimateTime {

	public long add = Long.MIN_VALUE;

	private class Time { 
		
		final long clock;
		final long ms;
		final long diff;
		
		public Time(final long clock, final long ms) {
			super();
			this.clock = clock;
			this.ms = ms;
			this.diff = ms - clock;
		}
	}
	
	private LinkedList<Time> times = new LinkedList<Time>();
	
	private Time min; 
	private Time max; 
	
	public void parseAll(File directory, String prefix, String [] clusters, 
			String postfix) throws IOException { 

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
				parseCluster(cluster, 
						nodes.toArray(new String[nodes.size()]), 
						files.toArray(new File[files.size()]));
				nodes.clear();
				files.clear();	
			}
		}
	}


	private void parseCluster(String cluster, String[] nodes, File[] files) throws IOException {
		for (int i=0;i<nodes.length;i++) { 
			parseNode(cluster, nodes[i], files[i]);
		}
	}

	private void parseNode(String cluster, String node, File file) throws IOException {
		// TODO Auto-generated method stub
		
		times.clear();
		min = null;
		max = null;
		
		BufferedReader r = new BufferedReader(new FileReader(file));

		try { 
			String line = r.readLine();

			while (line != null) { 
				parseLine(line);
				line = r.readLine();
			}
		} finally {  
			r.close();
		}

		long diff = max.diff - min.diff;
		
		System.out.println(node + " " + diff + " " + min.diff + " " + max.diff);
	}		

	public long getTime(String time) { 

		StringTokenizer tok = new StringTokenizer(time, ":");

		if (tok.countTokens() != 3) { 
			System.out.println("Cannot parse time! " + time);
			return 0;
		}

		long hour = Integer.parseInt(tok.nextToken());
		long minute = Integer.parseInt(tok.nextToken());
		long second = Integer.parseInt(tok.nextToken());

		long t = second + 60*minute + 60*60*hour;

		return t;
	}

	private void checkTimeDiff(Time t) { 
		
		if (min == null && max == null) { 
			min = t;
			max = t;
			return;
		}
	
		if (min.diff > t.diff) { 
			min = t;
			return;
		}
		
		if (max.diff < t.diff) { 
			max = t;
			return;
		}
	}
	
	private void parseLine(String line) { 

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

		if (line.contains("Completed")) { 

			long clocktime = getTime(tok.nextToken()) * 1000;
			
			skipTokens(tok, 9);

			long mstime = Long.parseLong(tok.nextToken());

			Time t = new Time(clocktime, mstime);
			
			checkTimeDiff(t);
			
			times.add(t);
		}
	}

	private void skipTokens(StringTokenizer tok, int num) { 
		for (int i=0;i<num;i++) { 
			tok.nextToken();
		}
	}

	public static void main(String [] args) { 

		File dir = new File(args[0]);
		String prefix = args[1];
		String postfix = args[2];

		String [] clusters = new String[args.length-3];
		System.arraycopy(args, 3, clusters, 0, args.length-3);		

		try {
			new EstimateTime().parseAll(dir, prefix, clusters, postfix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
