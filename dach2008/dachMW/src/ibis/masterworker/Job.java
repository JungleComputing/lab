package ibis.masterworker;

import java.io.Serializable;
import java.util.Set;

public abstract class Job implements Serializable, Comparable<Job> {

	private static long nextID = 0;
	
	private static synchronized long nextID() { 
		return nextID++;
	}
	
	public final long ID; 
	public final Set<String> preferredLocations; 
	
	protected Job(Set<String> preferredLocations) { 
		this.ID = nextID();
		this.preferredLocations = preferredLocations;
	}

	public abstract int score(String node);
	public abstract long getJobSize();
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (ID ^ (ID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Job other = (Job) obj;
		if (ID != other.ID)
			return false;
		return true;
	}

	public Set<String> getPreferredLocations() {
		return preferredLocations;
	}
	
	public boolean isPreferredLocation(String location) {
		return preferredLocations.contains(location);
	}
	
	public int compareTo(Job o) { 
		
		int result = preferredLocations.size() - o.preferredLocations.size();

///System.out.println("Compare0 = " + result);		
		
		if (result == 0) { 
			// They are the same size!
			result = preferredLocations.hashCode() - o.preferredLocations.hashCode();

//System.out.println("Compare1 = " + result);		
			
			if (result == 0) { 
				// They contain the same elements!
				if (getJobSize() < o.getJobSize()) { 
					result = 1;
				} else { 
					result = -1;
				}
			}
			
///System.out.println("Compare2 = " + result);		
			
		}
		
		return result;
	}
}
