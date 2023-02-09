package org.jahia.modules.lucenereindex.flow;

import java.io.Serializable;
import java.util.HashSet;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.context.ExternalContext;

public class ReindexManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3978511268436040678L;
	private static final Logger logger = LoggerFactory.getLogger(ReindexManager.class);

	public boolean isCluster() {
		return SettingsBean.getInstance().isClusterActivated();
	}

	private Object getClusterManager() {
		return BundleUtils.getOsgiService("org.apache.karaf.cellar.core.ClusterManager", null);
	}

	public Object getLocalClusterNode() {
		try {

			return MethodUtils.invokeExactMethod(getClusterManager(), "getNode", null);
		} catch (Exception ex) {
			logger.error("Error to get Cluster members", ex);
		}
		return null;

	}

	public HashSet getClusterNodes() {
		try {

			return (HashSet) MethodUtils.invokeExactMethod(getClusterManager(), "listNodes", null);
		} catch (Exception ex) {
			logger.error("Error to get Cluster members", ex);
		}
		return null;

		/*
		 * ClusterManager manager = BundleUtils.getOsgiService(ClusterManager.class,
		 * null); manager.listNodes();
		 */
	}

	private String getClusterId(Object clusternode) {
		try {

			return (String) MethodUtils.invokeExactMethod(clusternode, "getId", null);
		} catch (Exception ex) {
			logger.error("Error to get Cluster members", ex);
		}
		return null;
	}

	public void putNodeForReindex(ExternalContext context) {

		final String nodeToReindex = context.getRequestParameterMap().get("_eventId_addreindex");
		logger.info("Add Node " + nodeToReindex + " to reindexation!");
		try {
			JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
				@Override
				public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
					JCRNodeWrapper admin = session.getNode("/settings/reindexAdmin");
					for (JCRNodeWrapper node : admin.getNodes()) {
						if (node.getProperty("nodeId").getString().equals(nodeToReindex)) {

							logger.info("Node already in list to reindex");
							return null;

						}
					}
					// Add node
					JCRNodeWrapper indexNode = admin.addNode("reindex" + System.currentTimeMillis(),
							"jnt:nodeToReindex");
					indexNode.setProperty("nodeId", nodeToReindex);
					indexNode.saveSession();
					return null;
				}
			});
		} catch (RepositoryException ex) {
			logger.error("Cannot add node to reindex list", ex);
		}

	}

	public void reindexFull(ExternalContext context) {

		logger.info("Reindex all available cluster members!");
		final HashSet nodes = getClusterNodes();
		for (Object clusternode : nodes) {
			final String nodeToReindex = getClusterId(clusternode);
			try {
				JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
					@Override
					public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
						JCRNodeWrapper admin = session.getNode("/settings/reindexAdmin");
						for (JCRNodeWrapper node : admin.getNodes()) {
							if (node.getProperty("nodeId").getString().equals(nodeToReindex)) {

								logger.info("Node already in list to reindex");
								return null;

							}
						}
						// Add node
						JCRNodeWrapper indexNode = admin.addNode("reindex" + System.currentTimeMillis(),
								"jnt:nodeToReindex");
						indexNode.setProperty("nodeId", nodeToReindex);
						indexNode.saveSession();
						return null;
					}
				});

			} catch (RepositoryException ex) {
				logger.error("Cannot add node to reindex list", ex);
			}
		}

	}
}
