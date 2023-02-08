package org.jahia.modules.lucenereindex.service;

import java.util.Timer;
import java.util.TimerTask;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.jahia.services.JahiaAfterInitializationService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterReindexationService implements JahiaAfterInitializationService {

	private static Logger logger = LoggerFactory.getLogger(ClusterReindexationService.class);
	
	
	private long scanInterval = 30000;
	private Timer watchdog;	
	private String nodePath = "/settings/reindexAdmin";
	private boolean isIniialized = false;
	
	
	public boolean isIniialized() {
		return isIniialized;
	}

	public void setScanInterval(long scanInterval) {
		this.scanInterval = scanInterval;
	}

	private void initializeJCRNode() throws RepositoryException {
		
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException { 
            	
            	if (!session.nodeExists(nodePath)) {
            		
            		JCRNodeWrapper settingsNode = session.getNode("/settings");
            		JCRNodeWrapper adminNode = settingsNode.addNode("reindexAdmin", "jnt:reindexAdminInfo");
            		adminNode.saveSession();
            	}
            	
                return null;
            }
        });
		
	}
	
	@Override
	public void initAfterAllServicesAreStarted() throws JahiaInitializationException {
        if (!SettingsBean.getInstance().isClusterActivated()) {
        	logger.info("No Cluster active " + this.getClass().getName() + " not initialized");
        	isIniialized = false;
        	return;
        }
		try {
		initializeJCRNode();
		// start watchdog for monitoring
		watchdog = new Timer(true);
		watchdog.schedule(new TimerTask() {
			@Override
			public void run() {
				performReindexCheck();
			}
		}, 0, scanInterval);
		isIniialized = true;
		
		} catch (RepositoryException ex) {
			isIniialized = false;
			logger.error("Indexer couldn't be initialized!", ex);
		}
	}
		
		
private void performReindexCheck() {
	
	logger.debug("Start Perform reindex check");
	
	final ReindexManager mnger = new ReindexManager();
	
	try {
    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
        @Override
        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException { 
        	try {
            String currentId = (String)MethodUtils.invokeExactMethod(mnger.getLocalClusterNode(), "getId", null);
        	JCRNodeWrapper admin = session.getNode(nodePath);
        	if (admin.hasNodes()) {
        		for (JCRNodeWrapper node : admin.getNodes()) {
        			if (node.getProperty("nodeId").getString().equals(currentId)) {
        				
        				logger.info("Reindex started on node " + currentId);
        				node.remove();
        				node.saveSession();
        				//start reindexation in another Thread (schedule indexation now!)
        				((JahiaRepositoryImpl)((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository()).scheduleReindexing();
        				return null;    				
        				
        			}
        		}
        		
        		
        	}
        	} catch (Exception ex) {
        		logger.error("reindex perform has error", ex);
        	}
        	
            return null;
        }
    });

	} catch (RepositoryException ex) {
		logger.error("Check of reindexation failed on node "  + mnger.getLocalClusterNode(), ex);
	}
}

}
