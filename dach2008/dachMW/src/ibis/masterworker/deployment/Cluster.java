package ibis.masterworker.deployment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public class Cluster {

	public final String file; 
	
	public final String name; 
	
	public final String master; // Only set if direct == false
	
	public final String java; 
	
	public final int cores;
	
	public final LinkedList<String> nodes; 
	
	private String ID;
	
	private Cluster(String name, String file, String master, String java, 
			int cores,  LinkedList<String> nodes) { 
		this.name = name;
		this.file = file;
		this.master = master;
		this.java = java;
		this.cores = cores;
		this.nodes = nodes;
	}
	
	public void setID(String ID) { 
		this.ID = ID;
	}
	
	public String getID() { 
		return ID;
	}
	
	public String toString() { 		
		return name + " (master: " + master + ", nodes: " + nodes.size() + ")"; 
	}
	
	public boolean isHead(String node) { 
		return master.equals(node);
	}
	
	public boolean isWorker(String node) { 
		
		for (String n : nodes) { 
			if (node.equals(n)) { 
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isNode(String node) { 
		return isHead(node) || isWorker(node);
	}
	
	public static void parseLine(String node, LinkedList<String> nodes) throws Exception { 
		
		int rangeStart; 
		int rangeEnd;
		
		int index4 = node.indexOf(' ');

		int cores = 0;
		
		if (index4 < 0) { 
			// Single core
			cores = 1;
		} else { 
			
			try { 
				cores = Integer.parseInt(node.substring(index4+1).trim());
			} catch (Exception e) {
				// ignore -- handled in next 'if'
			}
			
			if (cores <= 0) { 
				throw new Exception("Illegal core value in \"" + node + "\"");
			}
			
			node = node.substring(0, index4).trim();			
		}

		int index1 = node.indexOf('[');
		int index2 = node.lastIndexOf(']');

		if (index1 < 0 || index2 < 0) {
			
			for (int c=0;c<cores;c++) { 
				// System.out.println("Adding node " + node + " (core " + c + ")"); 				
				nodes.add(node);
			}

			return;
		}
		
		String nodePreFix = node.substring(0, index1);
		String nodePostFix = node.substring(index2+1);

		int index3 = node.indexOf('-', index1);

		if (index3 < 0) {
			String value = node.substring(index1+1, index2);
			rangeStart = rangeEnd = Integer.parseInt(value);
			
			for (int c=0;c<cores;c++) {
				//System.out.printf("Adding node %s%s%s (core %d)\n", nodePreFix, formatNumber(i, 3), nodePostFix, c); 
				nodes.add(nodePreFix + formatNumber(rangeStart, 3) + nodePostFix);
			}
			
			return;
		}

		rangeStart = Integer.parseInt(node.substring(index1+1, index3));
		rangeEnd = Integer.parseInt(node.substring(index3+1, index2));

		for (int i=rangeStart;i<rangeEnd+1;i++) { 
			for (int c=0;c<cores;c++) {
				//System.out.printf("Adding node %s%s%s (core %d)\n", nodePreFix, formatNumber(i, 3), nodePostFix, c); 
				nodes.add(nodePreFix + formatNumber(i, 3) + nodePostFix);
			}
		}
	}	
	
	private static final String formatNumber(int value, int positions) { 
		
		String tmp = Integer.toString(value);
		
		while (tmp.length() < positions) { 
			tmp = "0" + tmp;
		}
		
		return tmp;
	}
	
	public static String readLine(BufferedReader reader) throws Exception { 
	
		String line = null;
		
		do { 
			line = reader.readLine();
		
			if (line == null) { 
				return null;
			}
			
			line = line.trim();
			
		} while (line.length() == 0 || line.startsWith("#"));

		return line;
	}
	
	public static Cluster read(String file) throws Exception { 
		
		BufferedReader reader = null;
		
		String name = null;		
		String master = null;
		int cores = 0;
		
		String java = null;
		
		LinkedList<String> nodes = new LinkedList<String>();
		
		try {
			reader = new BufferedReader(new FileReader(file));
		
			name = readLine(reader);
			
			if (name == null) { 
				throw new Exception("Failed to read cluster file " + file + " - no name entry ?");
			}
			
			master = readLine(reader);
			
			if (master == null) {  
				throw new Exception("Failed to read cluster file " + file + " - no master entry ?");
			}
			
			java = readLine(reader);
			
			if (java == null) {  
				throw new Exception("Failed to read cluster file " + file + " - no java entry ?");
			}
		
			String tmp = readLine(reader); 
			
			if (tmp == null) {  
				throw new Exception("Failed to read cluster file " + file + " - no valid cores entry ?");
			}
			
			try { 
				cores = Integer.parseInt(tmp);
			} catch (Exception e) {
				throw new Exception("Failed to read cluster file " + file + " - no valid cores entry ?", e);	
			}
			
			String line = readLine(reader);
			
			while (line != null) { 
				
				line = line.trim();
				
				if (line.length() > 0) {
					parseLine(line, nodes);
				} else { 
					System.out.println("Empty line in cluster file " + file);
				}
					
				line = readLine(reader);	
			}
		} catch (Exception e) {
			throw new Exception("Failed to read cluster file " + file, e);
		} finally { 
			try { 
				if (reader != null) { 
					reader.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		
		if (nodes.size() == 0) {
			System.err.println("Cluster " + file + " - no nodes defined ?");
		}

		return new Cluster(name, file, master, java, cores, nodes);	
	}

	public static int maxSize(List<Cluster> clusters) {
		
		int size = 0;
		
		for (Cluster c : clusters) {
			size = Math.max(size, c.nodes.size());
		}
		
		return size;
	}

	
}
