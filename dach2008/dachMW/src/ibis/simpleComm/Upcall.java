package ibis.simpleComm;

import ibis.ipl.IbisIdentifier;

public interface Upcall {
	boolean upcall(IbisIdentifier src, Object o);
	void died(IbisIdentifier src);
}
