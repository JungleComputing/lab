package dach.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class DuplicateFiles {

	public static void main(String [] args) { 
		
		String src = null;
		String dst = null;
	
		int dup = 0;
		
		ArrayList<String> targets = new ArrayList<String>();
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-src") && i < args.length-1) { 
				src = args[++i];
			} else if (args[i].equals("-dst") && i < args.length-1) { 
				dst = args[++i];
			} else if (args[i].equals("-dup") && i < args.length-1) { 
				dup = Integer.parseInt(args[++i]);
			} else { 
				targets.add(args[i]);
			} 
		}
		
		if (src == null) { 
			System.err.println("Source directory not set!");
			System.exit(1);
		}
		
		if (dst == null) { 
			System.err.println("Destination directory not set!");
			System.exit(1);
		}
		
		if (targets.size() == 0) { 
			System.err.println("No targets specified!");
			System.exit(1);
		}
		
		if (dup <= 0) { 
			System.err.println("Duplication not set!");
			System.exit(1);
		}
		
		for (int i=0;i<targets.size();i++) { 
			
			System.out.print("ssh " + targets.get(i) + " dup " + src + " " + dst);
			
			for (int d=0;d<dup;d++) {
				
				int index = (i + d + 1) % targets.size();
				System.out.print(" " + targets.get(index));
			}
			
			System.out.println(" &");
		}
		
		
		
	}
	
	
}
