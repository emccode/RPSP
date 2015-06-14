package com.emc.rpsp.fal;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

import com.emc.fapi.jaxws.ClusterVirtualInfrastructuresState;
import com.emc.fapi.jaxws.ClusterVirtualInfrastructuresStateSet;
import com.emc.fapi.jaxws.ConsistencyGroupCopySettingsSet;
import com.emc.fapi.jaxws.ConsistencyGroupCopySnapshots;
import com.emc.fapi.jaxws.ConsistencyGroupSnapshots;
import com.emc.fapi.jaxws.ConsistencyGroupStateSet;
import com.emc.fapi.jaxws.ConsistencyGroupUIDSet;
import com.emc.fapi.jaxws.ConsistencyGroupVolumesStateSet;
import com.emc.fapi.jaxws.DisableImageAccessResponse;
import com.emc.fapi.jaxws.EnableImageAccessParams;
import com.emc.fapi.jaxws.EnableImageAccessResponse;
import com.emc.fapi.jaxws.EnableLatestImageAccessParams;
import com.emc.fapi.jaxws.EnableLatestImageAccessResponse;
import com.emc.fapi.jaxws.FullRecoverPointSettings;
import com.emc.fapi.jaxws.RecoverPointClustersInformation;
import com.emc.fapi.jaxws.RecoverPointTimeStamp;
import com.emc.fapi.jaxws.RecoverPointVersion;

/**
 * Created by morand3 on 1/13/2015.
 */
public interface ClusterConnector {
	static final String BASE_URL = "/";

	@GET(BASE_URL + "system/version")
	public RecoverPointVersion getSystemVersion();

	@GET(BASE_URL + "settings")
	public FullRecoverPointSettings getFullRecoverPointSettings();

	@GET(BASE_URL + "time/current_time")
	public RecoverPointTimeStamp getSystemTime();

	@GET(BASE_URL + "system/virtual_infrastructures/state")
	public ClusterVirtualInfrastructuresStateSet getVirtualInfrastructuresStateFromAllCluster();

	@GET(BASE_URL + "clusters/{clusterId}/virtual_infrastructures/state")
	public ClusterVirtualInfrastructuresState getVirtualInfrastructuresStateFromCluster(
	        @Path("clusterId") long clusterId);

	@GET(BASE_URL + "clusters")
	public RecoverPointClustersInformation getRpClustersInformation();

	@GET(BASE_URL + "groups/{groupId}/copies/settings")
	public ConsistencyGroupCopySettingsSet getAllGroupCopies(
	        @Path("groupId") long groupId);

	@GET(BASE_URL + "groups")
	public ConsistencyGroupUIDSet getAllGroupsUIDs();
	
	
	
	@PUT(BASE_URL + "groups/{groupId}/clusters/{clusterId}/copies/{copyId}/image_access/latest/enable")
	public EnableLatestImageAccessResponse enableLatestImageAccess(
			@Path("clusterId") long clusterId, @Path("groupId") long groupId, @Path("copyId") int copyId, @Body EnableLatestImageAccessParams params);
	
	@PUT(BASE_URL + "groups/{groupId}/clusters/{clusterId}/copies/{copyId}/enable_image_access")
	public EnableImageAccessResponse enableSnapshotImageAccess(
			@Path("clusterId") long clusterId, @Path("groupId") long groupId, @Path("copyId") int copyId, @Body EnableImageAccessParams params);
	
	@PUT(BASE_URL + "groups/{groupId}/clusters/{clusterId}/copies/{copyId}/disable_image_access?startTransfer=true")
	public DisableImageAccessResponse disableLatestImageAccess(
			@Path("clusterId") long clusterId, @Path("groupId") long groupId, @Path("copyId") int copyId);
	
	@GET(BASE_URL + "groups/state")
	public ConsistencyGroupStateSet getConsistencyGroupStateSet();
	
	@GET(BASE_URL + "groups/volumes/state")
	public ConsistencyGroupVolumesStateSet getConsistencyGroupVolumesStateSet();
	
	@GET(BASE_URL + "groups/{groupId}/snapshots")
	public ConsistencyGroupSnapshots getGroupSnapshots(@Path("groupId") long groupId, 
			                             @Query("startTime") Long startTime, 
			                                         @Query("endTime") Long endTime);
	
	
	

}
