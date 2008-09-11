package ibis.dfs;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class FileInfo implements Serializable {
	
	private static final long serialVersionUID = -1393754777305462517L;

	/** The local filename */
	public final String name;

	/** The size of the file. */
	public final long size;

	/** The hosts where this file is replicated */
	public Set<String> replicaHosts = null;
	
	/** The sites where this file is replicated */
	public Set<String> replicaSites = null;
	
	public FileInfo(String name, long size) { 
		this.name = name;
		this.size = size;
	}

	private void addReplicaSite(String sites) { 
		
		if (replicaSites == null) { 
			replicaSites = new HashSet<String>();
		}
		
		replicaSites.add(sites);
	}
	
	private String getDomain(String host) { 
		
		if (host == null) { 
			return null;
		}
		
		host = host.trim();
		
		int index = host.indexOf('.');
		
		if (index <= 0) { 
			return null;
		}
	
		return host.substring(index+1);
	}

	public void addReplicaHost(String host) { 
		if (replicaHosts == null) { 
			replicaHosts = new HashSet<String>();
		}
		
		replicaHosts.add(host);
		
		addReplicaSite(getDomain(host));
	}
	
	public void addReplicaHosts(Set<String> hosts) { 
		
		if (replicaHosts == null) { 
			replicaHosts = new HashSet<String>();
		}
		
		replicaHosts.addAll(hosts);
		
		for (String s : hosts) { 
			addReplicaSite(getDomain(s));
		}
	}
	
	public boolean onHost(String host) { 
		
		if (replicaHosts == null) { 
			return false;
		}
		
		return replicaHosts.contains(host);		
	}
	
	public boolean onSiteOfHost(String host) { 
		return onSite(getDomain(host));
	}
		
	public boolean onSite(String site) { 
		
		if (replicaSites == null) { 
			return false;
		}
		
		return replicaSites.contains(site);		
	}

	public int score(String host) {
		
		if (onHost(host)) { 
			return 4;	
		}
		
		if (onSiteOfHost(host)) { 
			return 2;
		}
		
		return 1;
	}

	public String selectReplica(Set<String> usedHosts, Set<String> usedDomains) {

		// TODO: way to heavy weight!
		if (replicaHosts.size() == 0) { 
			return null;
		}
		
		if (usedHosts.size() == 0) { 
			LinkedList<String> options = new LinkedList<String>(replicaHosts); 
			Collections.shuffle(options);	
			
			String host = options.getFirst();
			
			usedHosts.add(host);
			usedDomains.add(getDomain(host));
			
			return host;
		}
		
		Set<String> tmp = new HashSet<String>(replicaHosts);
		tmp.removeAll(usedHosts);
		
		if (tmp.isEmpty()) { 
			return null;
		}

		LinkedList<String> options = new LinkedList<String>(); 
		
		for (String s : tmp) {
			if (!usedDomains.contains(getDomain(s))) { 
				// We've found a host in an unused domain!
				options.add(s);
			}
		}

		if (options.size() == 0) { 
			options.addAll(tmp);
		}
		
		Collections.shuffle(options);

		String host = options.getFirst();
		
		usedHosts.add(host);
		usedDomains.add(getDomain(host));

		return host;
	}
	
	public String toString() { 
		return name + " " + size + " " + replicaHosts; 
	}
}
