package com.emc.rpsp.virtualconfig.domain;

import java.util.ArrayList;
import java.util.List;

public class DataCenterConfig {
	
	private String id;
	private String name;
	private List<EsxClusterConfig> esxClustersConfig = new ArrayList<EsxClusterConfig>();
	private List<DataStoreConfig> datastores = new ArrayList<DataStoreConfig>();
	
	public DataCenterConfig() {
		super();
	}	


	public DataCenterConfig(String id, String name,
			List<EsxClusterConfig> esxClustersConfig) {
		super();
		this.id = id;
		this.name = name;
		this.esxClustersConfig = esxClustersConfig;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<EsxClusterConfig> getEsxClustersConfig() {
		return esxClustersConfig;
	}

	public void setEsxClustersConfig(List<EsxClusterConfig> esxClustersConfig) {
		this.esxClustersConfig = esxClustersConfig;
	}
	
	
	public void addEsxClusterConfig(EsxClusterConfig esxClusterConfig) {
		this.esxClustersConfig.add(esxClusterConfig);
	}
	

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public List<DataStoreConfig> getDatastores() {
		return datastores;
	}


	public void setDatastores(List<DataStoreConfig> datastores) {
		this.datastores = datastores;
	}
	
	public void addDatastore(DataStoreConfig dataStoreConfig){
		this.datastores.add(dataStoreConfig);
	}


	@Override
	public String toString() {
		return "DataCenterConfig [id=" + id + ", name=" + name
				+ ", esxClustersConfig=" + esxClustersConfig + "]";
	}

	
	
	
	
}
