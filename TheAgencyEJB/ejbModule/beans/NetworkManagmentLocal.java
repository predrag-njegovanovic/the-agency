package beans;

import javax.ejb.Local;

@Local
public interface NetworkManagmentLocal {

	public boolean isMaster();
	public String getMasterAddress();
}
