package dach.gui;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import dach.gui.NodeStatistics.Event;

public class ShowWorkGNUPlot {

	private LinkedList<Long> jobs = new LinkedList<Long>();
	
	private int tcount = 0;
	private int ccount = 0;
	
	public ShowWorkGNUPlot(LinkedList<ClusterStatistics> stats, long appEndTime) {

		int totalLines = 0;

		for (ClusterStatistics c : stats) { 
			totalLines += c.cores * c.nodes.size() + c.nodes.size() * 4;
		}

		totalLines += stats.size() * 4;

		long endTime = 0;
		long jobEndTime = 0;

		for (ClusterStatistics c : stats) { 
			totalLines += c.getNodes();

			jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
			endTime = Math.max(endTime, c.getLatestEndTime());
		}

		System.out.println("set term postscript eps enhanced");
		System.out.println("set output \"eep.eps\"");
		System.out.println("set font \"Helvetica\"");
		System.out.println("unset key"); 
		System.out.println("set xlabel \"Time (s)\"");
		System.out.println("set ylabel \"Cores\"");
		System.out.println("set size 2.5, 1.0"); 
		System.out.println("set style line 1 lt 4 lw 0.5");
		System.out.println("set style line 2 lt 1 lw 2");
		System.out.println("set samples 20000");
		System.out.println("set multiplot");

		int line = 10;

		for (ClusterStatistics c : stats) { 

			line += drawCluster(c, line);
			line += 20;
		}

		line += 20;

		
		/*
		for (Long job : jobs) {
	
			
			
			/*
			System.out.println("plot [0:" + (jobEndTime+100) + "][0:" + line + "] c" 
					+ job + "(x) w l ls 1");

			System.out.println("plot [0:" + (jobEndTime+100) + "][0:" + line + "] t" 
					+ job + "(x) w l ls 2");
*/
/*			
			System.out.println("c" + job + "(x) w l ls 1, \\");
			System.out.println("t" + job + "(x) w l ls 2, \\");
	
			if (count++ > 100) { 
				break;
			}*/
		// }
		
		for (int i=0;i<ccount;i++) { 
			System.out.println("plot [0:" + (jobEndTime+100) + "][0:" + line + "] c" 
					+ i + "(x) w l ls 1");
		}
		
		for (int i=0;i<tcount;i++) { 
			System.out.println("plot [0:" + (jobEndTime+100) + "][0:" + line + "] t" 
					+ i + "(x) w l ls 2");
		}
	}

	private int drawNodeMC(int cores, NodeStatistics n, final int line) { 

		int myLine = line;

		int start = (int) n.getEarliestStartTime();
		int end = (int) n.getLatestEndTime();

		System.out.print("t" + n.node + "(x) = "); 
		
		for (NodeStatistics.Event e : n.events) { 

			switch (e.type) { 
			case STEAL_REQUEST:
			case STEAL_REPLY_EMPTY:			
			case STEAL_REPLY_DONE:			
			case STEAL_REPLY_FULL:
			case START_COMPUTE:
				break;
			case JOB_RESULT:

				jobs.add(e.ID);
				
				long [] data = (long[]) e.data;

				int jobTime = (int) Math.round(data[0] / 1000.0);
				int computeTime = (int) Math.round(data[4] / 1000.0);
				int transferTime = (int) Math.round(data[1] / 1000.0);

				int startT = (int) (e.time - jobTime);
				int computeT = (int) (e.time - computeTime);
				
				System.out.print(startT + "<=x && x<=" + computeT + " ? " + myLine + " : ");
/*
				System.out.println("t" + e.ID + "(x) = " + startT + "<=x && x<=" 
						+ computeT + " ? " + myLine + " : 1/0");

				System.out.println("c" + e.ID + "(x) = " + computeT + "<=x && x<=" 
						+ (e.time) + " ? " + myLine + " : 1/0");
*/
			}
			
			System.out.println(" 1/0 ");
		}

		myLine += cores;

		return (myLine - line);	
	}

	private static class TimeSorter implements Comparator<NodeStatistics.Event> {

		private long getStartTime(Event e) { 
			long [] data = (long[]) e.data;
			int jobTime = (int) Math.round(data[0] / 1000.0);
			return (e.time - jobTime);
		}

		public int compare(Event arg0, Event arg1) {

			long t0 = getStartTime(arg0);
			long t1 = getStartTime(arg1);

			return (int) (t0-t1);
		}
	}

	private int drawNodeSC(int cores, NodeStatistics n, final int line) { 

		long [] activity = new long[cores];

		Arrays.fill(activity, Long.MIN_VALUE);

		int myLine = line;

		int start = (int) n.getEarliestStartTime();
		int end = (int) n.getLatestEndTime();

		LinkedList<NodeStatistics.Event> tmp = new LinkedList<NodeStatistics.Event>();

		for (NodeStatistics.Event e : n.events) { 
			if (e.type == NodeStatistics.EventType.JOB_RESULT) { 
				tmp.add(e);
			}
		}

		Collections.sort(tmp, new TimeSorter()); 
		
		StringBuilder [] sb = new StringBuilder[cores];
		
		for (int i=0;i<cores;i++) { 
			sb[i] = new StringBuilder();
			sb[i].append("t" + (tcount++) + "(x) = "); 
		}
		
		for (NodeStatistics.Event e : tmp) { 

			jobs.add(e.ID);
			
			long [] data = (long[]) e.data;

			int jobTime = (int) Math.round(data[0] / 1000.0);
			int computeTime = (int) Math.round(data[4] / 1000.0);
			int transferTime = (int) Math.round(data[1] / 1000.0);

			int startT = (int) (e.time - jobTime);
			int computeT = (int) (e.time - computeTime);

			int index = -1;

			if (startT < 0) { 
				startT = 0;
			}

			for (int i=0;i<cores;i++) { 

				if (activity[i] <= startT) { 
					activity[i] = startT + jobTime-3;
					index = i;
					break;
				}
			}

			if (index == -1) { 
				System.err.println("EEP failed to find empty core slot! " + startT + " " 
						+ Arrays.toString(activity) + " " + e.ID);
				System.exit(1);
			}

			sb[index].append(startT + "<=x && x<=" 
						+ computeT + " ? " + (myLine+index) + " : ");
		
			/*
			System.out.println("t" + e.ID + "(x) = " + startT + "<=x && x<=" 
					+ computeT + " ? " + (myLine+index) + " : 1/0");

			System.out.println("c" + e.ID + "(x) = " + computeT + "<=x && x<=" 
					+ (e.time) + " ? " + (myLine+index) + " : 1/0");
			 */
		}
	
		System.out.println("c" + ccount++ + "(x) = " + start + "<=x && x<=" 
				+ end + " ? " + (myLine+(cores/2.0)) + " : 1/0");
		
		for (int i=0;i<cores;i++) { 
		//	System.out.println("c" + ccount++ + "(x) = " + start + "<=x && x<=" 
		//			+ end + " ? " + (myLine+i) + " : 1/0");
			
			sb[i].append("1/0"); 
			System.out.println(sb[i]);
		
		}
			
		myLine += cores;

		return (myLine - line);	
	}

	private int drawCluster(ClusterStatistics c, final int line) { 

		int myLine = line;

		for (NodeStatistics n : c.nodes) { 

			if (c.runMultiCore) { 
				myLine += drawNodeMC(c.cores, n, myLine);
				myLine +=4;
			} else { 
				myLine += drawNodeSC(c.cores, n, myLine);
				myLine +=4;
			}
		}

		return (myLine - line);
	}

	public static void main(String [] args) { 

		if (args.length < 5) { 
			System.err.println("Usage: OutputParser.main time dir prefix postfix cluster+");
			System.exit(1);
		}

		OutputParser p = new OutputParser(args[0], false);

		long startTime = p.getTime(args[0]); 
		long endTime = p.getTime(args[1]); 

		File dir = new File(args[2]);
		String prefix = args[3];
		String postfix = args[4];

		String [] clusters = new String[args.length-5];
		System.arraycopy(args, 5, clusters, 0, args.length-5);				

		try {
			LinkedList<ClusterStatistics> s = p.parseAll(dir, startTime, prefix, clusters, postfix);

			System.err.println("Got " + s.size() + " clusters");

			long nodeEndTime = 0;
			long jobEndTime = 0;

			for (ClusterStatistics c : s) { 
				System.err.println("  " + c.name + " " + c.nodes.size() + " nodes " + c.getJobs() + " jobs");

				jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
				nodeEndTime = Math.max(nodeEndTime, c.getLatestEndTime());
			}

			System.err.println("total time = " + nodeEndTime);
			System.err.println("total job time = " + jobEndTime);

			System.err.println("total work time = " + p.totalWorkTime);
			System.err.println("total compute time = " + p.totalComputeTime);
			System.err.println("total transfer time = " + (p.totalWorkTime - p.totalComputeTime));

			new ShowWorkGNUPlot(s, endTime-startTime);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
