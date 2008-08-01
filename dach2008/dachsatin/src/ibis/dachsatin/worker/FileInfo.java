package ibis.dachsatin.worker;

import java.io.Serializable;
import java.util.HashSet;
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
		
		replicaHosts.add(sites);
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
}
