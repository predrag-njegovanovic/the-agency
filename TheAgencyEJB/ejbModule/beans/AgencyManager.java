package beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Singleton;

import model.Agent;
import model.AgentType;

@Singleton
public class AgencyManager implements AgencyManagerLocal {

	private List<Agent> runningAgents;
	private Set<AgentType> supportedTypes; 
	private Map<String,Set<AgentType>> otherSupportedTypes;
	
	public AgencyManager() { }
	
	public void initialise(){
		this.runningAgents  = new LinkedList<Agent>();
		this.supportedTypes = new HashSet<AgentType>();
		this.otherSupportedTypes = new HashMap<String, Set<AgentType>>();
	}
	
	public Set<AgentType> getSupportedTypes() {
		return supportedTypes;
	}

	public void setSupportedTypes(Set<AgentType> supportedTypes) {
		this.supportedTypes = supportedTypes;
	}

	public List<Agent> getRunningAgents() {
		return runningAgents;
	}

	public void setRunningAgents(List<Agent> runningAgents) {
		this.runningAgents = runningAgents;
	}

	public Map<String, Set<AgentType>> getOtherSupportedTypes() {
		return otherSupportedTypes;
	}

	public void setOtherSupportedTypes(Map<String, Set<AgentType>> otherSupportedTypes) {
		this.otherSupportedTypes = otherSupportedTypes;
	}
	
	public boolean isContained(String alias){
		return otherSupportedTypes.containsKey(alias);
	}
	
	public void addOtherTypes(String alias, Set<AgentType> types){
		if(!isContained(alias))
			otherSupportedTypes.put(alias, types);
		else
			otherSupportedTypes.get(alias).addAll(types);
	}
	
	public void deleteOtherTypes(String alias){
		otherSupportedTypes.remove(alias);
	}
	
	
}