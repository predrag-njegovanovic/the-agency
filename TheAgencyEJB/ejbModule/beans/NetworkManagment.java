package beans;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import exceptions.ConnectionException;
import handshake.HandshakeRequesterLocal;
import handshake.HandshakeResponseLocal;
import model.AgentCenter;
import model.HandshakeMessage;
import model.HandshakeMessage.handshakeType;
import util.PortTransformation;

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Local(NetworkManagmentLocal.class)
public class NetworkManagment implements NetworkManagmentLocal{
	
	private final int PORT = 8080;
	private final String MASTER = "master";
	private final String ALIAS  = "alias";
	private final String OFFSET = "jboss.socket.binding.port-offset";
	private final String TYPES  = "types";
	private final String LOCAL  = "local";
	
	private boolean master;
	private String masterIpAddress;
	
	
	@EJB
	private AgencyRegistryLocal registryBean;
	
	@EJB
	private HandshakeRequesterLocal requester;
	
	@EJB
	private HandshakeResponseLocal timer;
	
	@EJB 
	private AgencyManagerLocal agency;
	
	@PostConstruct
	public void initialise(){		
		if(System.getProperty(MASTER) == null){
			master = true;
			try {
				registryBean.setThisCenter(createCenter());
				System.out.println("MASTER NODE UP");
				timer.startTimer();
			} catch (UnknownHostException e) {
				//TODO: shutdown script
			}
			return;
		}
		masterIpAddress   = System.getProperty(MASTER);
		AgentCenter slave = null;
		try {
			slave = createCenter();
			System.out.println("SLAVE NODE UP "+slave.getAlias());
			registryBean.setThisCenter(slave);
			master = false;
			timer.startTimer();
			
			try {
				HandshakeMessage message = sendMessageToMaster(masterIpAddress, slave);
				if(message != null) message.getCenters().forEach(center -> registryBean.addCenter(center));
			} catch (ConnectionException e) {
				try {
					HandshakeMessage message = sendMessageToMaster(masterIpAddress, slave);
					if(message != null) message.getCenters().forEach(center -> registryBean.addCenter(center));
				} catch (ConnectionException e1) {
					try {
						HandshakeMessage message = rollback(masterIpAddress, slave);
						if(message != null) shutdownServer();
					} catch (ConnectionException e2) {
						System.out.println("Handshake failed. Rollback failed. Shuting down server...");
						shutdownServer();
					}
				}
			}
			
			try {
				HandshakeMessage message = getAllAgentTypes(masterIpAddress, slave);
				if(message != null) message.getOtherTypes().entrySet()
														   .stream()
														   .forEach(entry -> agency.addOtherTypes(entry.getKey(), entry.getValue()));
			} catch (ConnectionException e) {
				try {
					HandshakeMessage message = getAllAgentTypes(masterIpAddress, slave);
					if(message != null) message.getOtherTypes().entrySet()
															   .stream()
															   .forEach(entry -> agency.addOtherTypes(entry.getKey(), entry.getValue()));
				} catch (ConnectionException e1) {
					try {
						HandshakeMessage message = rollback(masterIpAddress, slave);
						if(message != null) shutdownServer();
					} catch (ConnectionException e2) {
						System.out.println("Handshake failed. Rollback failed. Shuting down server...");
						shutdownServer();
					}
				}
			}
			try {
				HandshakeMessage message = getAllRunningAgents(masterIpAddress, slave);
				if(message != null) message.getRunningAgents().addAll(message.getRunningAgents());
			} catch (ConnectionException e) {
				try {
					HandshakeMessage message = getAllRunningAgents(masterIpAddress, slave);
					if(message != null) message.getRunningAgents().addAll(message.getRunningAgents());
				} catch (ConnectionException e1) {
					try {
						HandshakeMessage message = rollback(masterIpAddress, slave);
						if(message != null) shutdownServer();
					} catch (ConnectionException e2) {
						System.out.println("Handshake failed. Rollback failed. Shuting down serve...");
						shutdownServer();
					}
				}
			}
			
		} catch (UnknownHostException e) {
			shutdownServer();
		}
		
		
		
	}
	
	private AgentCenter createCenter() throws UnknownHostException{
		String ipAddress = System.getProperty(LOCAL) == null ? "127.0.0.1" : System.getProperty(LOCAL);
		String alias     = System.getProperty(ALIAS) == null ? InetAddress.getLocalHost().getHostName() : System.getProperty(ALIAS);
		int offset 		 = System.getProperty(OFFSET) == null ? 0 : Integer.parseInt(System.getProperty(OFFSET));
		String filename  = System.getProperty(TYPES) == null ? "/default" : System.getProperty(TYPES);
		AgentCenter center = new AgentCenter(alias, ipAddress+":"+(PORT+offset));
		agency.setSupportedTypes(PortTransformation.agentTypes(filename));
		return center;
	}
		
	private HandshakeMessage sendMessageToMaster(String masterIpAddress, AgentCenter slave) throws ConnectionException{
		return requester.sendMessage(masterIpAddress, new HandshakeMessage(slave, handshakeType.REGISTER));
	}
	
	private HandshakeMessage getAllAgentTypes(String masterIpAddress, AgentCenter slave) throws ConnectionException{
		return requester.sendMessage(masterIpAddress, createMessage(slave, handshakeType.GET_TYPES));
	}
	
	private HandshakeMessage getAllRunningAgents(String masterIpAddress, AgentCenter slave) throws ConnectionException{
		return requester.sendMessage(masterIpAddress, createMessage(slave, handshakeType.GET_RUNNING));
	}

	private HandshakeMessage rollback(String masterIpAddress, AgentCenter slave) throws ConnectionException{
		return requester.sendMessage(masterIpAddress, createMessage(slave, handshakeType.ROLLBACK));
	}
	
	private HandshakeMessage createMessage(AgentCenter slave, handshakeType type){
		HandshakeMessage message = new HandshakeMessage();
		message.setType(type);
		message.setAgentTypes(agency.getSupportedTypes());
		message.setCenter(slave);
		message.setRunningAgents(agency.getRunningAgents());
		return message;
	}
	
	private void shutdownServer(){
		//TODO: Load script and exec it
	}
	
	public boolean isMaster(){
		return master;
	}
	
	public String getMasterAddress(){
		return masterIpAddress;
	}
}
