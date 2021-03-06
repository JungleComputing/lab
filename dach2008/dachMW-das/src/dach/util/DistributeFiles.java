package dach.util;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class DistributeFiles {

	private final File dir; 
	private final String dst;
	private final String domain;
	
	private final int parallel = 10;
	private final boolean pull = true;
	private final String srcMachine = "carrot.cs.vu.nl";
	
	private final LinkedList<String> targets;
	
	private final HashMap<String, File> files = new HashMap<String, File>();
	
	private final LinkedList<Pair> pairs = new LinkedList<Pair>();
	
	final class Pair { 
		
		public File f1; 
		public File f2;
	
		public Pair(final File f1, final File f2) {
			this.f1 = f1;
			this.f2 = f2;
		}
		
	}
	
	public DistributeFiles(File dir, String dst, String domain, LinkedList<String> targets) {
		this.dir = dir;
		this.dst = dst;
		this.domain = domain;
		this.targets = targets;
	}

	public void start() { 
		
		File [] tmp = dir.listFiles();
		
		for (File f : tmp) {
			
			String name = f.getName();
			
			if (name.endsWith("t0.fits")) { 
				
				String other = name.substring(0, name.length()-7) + "t1.fits";
				
				File o = files.remove(other);
				
				if (o != null) {
					pairs.add(new Pair(f, o));
				} else { 
					// The other file has not been seen yet!
					files.put(name, f);
				}
			} else if (name.endsWith("t1.fits")) {
		
				String other = name.substring(0, name.length()-7) + "t0.fits";
				
				File o = files.remove(other);
				
				if (o != null) {
					pairs.add(new Pair(o, f));
				} else { 
					// The other file has not been seen yet!
					files.put(name, f);
				}
			} else { 
				System.err.println("Cannot handle file: " + f);
			}
		}
	
		if (!files.keySet().isEmpty()) { 
			System.err.println("Leftover files: ");
			
			for (String s : files.keySet()) { 
				System.err.println("   " + s);
			} 
		}
		
		System.err.println("Found pairs: ");
		
		for (Pair p : pairs) { 
			System.err.println("  " + p.f1.getName() + " " + p.f2.getName());
		}
	
		int div = pairs.size() / targets.size(); 
		int mod = pairs.size() % targets.size(); 
		
		if (div == 0) { 
			System.err.println("WARN: Not enough pairs to fill all nodes!");
		}
		
		int index = 0;

		System.out.println("#!/bin/sh");
	
		/*
		for (String node : targets) { 
			
			String files = "";
			
			for (int i=0;i<div;i++) { 
				Pair p = pairs.removeFirst();
				files += p.f1.getPath() + " " + p.f2.getPath() + " ";
			} 
			
			if (index < mod) { 
				Pair p = pairs.removeFirst();
				files += p.f1.getPath() + " " + p.f2.getPath() + " ";
			}
			
			System.out.println("echo Copying to " + node);
			
			if (!pull) {
				System.out.println("scp " + files + " " + node + domain + ":" + dst + (parallel > 1 ? " &": ""));
			} else { 
				System.out.println("ssh " + node + domain + " scp " + srcMachine + ":\"\\\"" + files + "\\\"\" " + dst + (parallel > 1 ? " &": ""));	
			}
			
			index++;
			
			if (parallel > 1 && index % parallel == 0) { 
				System.out.println("wait");
			}
		}*/
		
		Iterator<String> nodes = targets.iterator();
		
		for (Pair p : pairs) { 
			
			if (!nodes.hasNext()) { 
				 nodes = targets.iterator();
			}
			
			String target = nodes.next();
			
			System.out.println("echo Copying to " + target + " " + index + " of " + pairs.size());
			System.out.println("ssh " + target + domain + " scp " + srcMachine + ":\"\\\"" 
					+ p.f1.getPath() + " " + p.f2.getPath() + "\\\"\" " 
					+ dst + (parallel > 1 ? " &": ""));	
			
			index++;
			
			if (parallel > 1 && index % parallel == 0) { 
				System.out.println("wait");
			}
		}

		if (parallel > 1) { 
			System.out.println("wait");
		}

	}
	
	public static void main(String [] args) { 
		
		File dir = null;
		String dst = null;
		String domain = "";
		
		LinkedList<String> targets = new LinkedList<String>();
		
		for (int i=0;i<args.length;i++) { 
			
			if (args[i].equals("-src") && i < args.length-1) { 
				dir = new File(args[++i]);
			} else if (args[i].equals("-dst") && i < args.length-1) { 
				dst = args[++i];
			} else if (args[i].equals("-domain") && i < args.length-1) { 
				domain = args[++i];	
			} else { 
				targets.add(args[i]);
			} 
		}
		
		if (dir == null || !dir.exists() || !dir.isDirectory() || !dir.canRead()) { 
			System.err.println("Cannot read source directory " + dir);
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
		
		new DistributeFiles(dir, dst, domain, targets).start();
	}
	
	
}
