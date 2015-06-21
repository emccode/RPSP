package com.emc.rpsp.vmstructure.service.impl;

import com.emc.fapi.jaxws.*;
import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.fal.Client;
import com.emc.rpsp.rpsystems.SystemSettings;
import com.emc.rpsp.users.service.UserService;
import com.emc.rpsp.vms.domain.VmOwnership;
import com.emc.rpsp.vmstructure.constants.ImageAccess;
import com.emc.rpsp.vmstructure.constants.TransferState;
import com.emc.rpsp.vmstructure.domain.*;
import com.emc.rpsp.vmstructure.service.AccountVmsStructureService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service public class AccountVmsStructureServiceImpl implements AccountVmsStructureService {

    @Autowired private UserService userService = null;

    @Override public AccountVmsStructure getAccountVmsStrucure() {

        AccountVmsStructure accountVmsStructure = new AccountVmsStructure();
        Account account = userService.findCurrentUser().getUser().getAccount();

        if (account != null) {
            accountVmsStructure.setId(account.getId().toString());
            accountVmsStructure.setName(account.getName());
            List<SystemSettings> systems = account.getSystemSettings();
            for (SystemSettings currSystem : systems) {
                Client client = new Client(currSystem);
                AccountVmsStructure currAccountVmsStructure = getAccountVmsStrucure(client, account,
                currSystem);
                accountVmsStructure.getUnprotectedVms()
                .addAll(currAccountVmsStructure.getUnprotectedVms());
                accountVmsStructure.getProtectedVms()
                .addAll(currAccountVmsStructure.getProtectedVms());
            }
        }
        return accountVmsStructure;
    }

    private AccountVmsStructure getAccountVmsStrucure(Client client, Account account,
    SystemSettings currSystem) {

        AccountVmsStructure accountVmsStructure = new AccountVmsStructure();
        List<VmContainer> protectedVms = new LinkedList<VmContainer>();

        FullRecoverPointSettings rpSettings = client.getFullRecoverPointSettings();

        Map<String, VmOwnership> vmsMap = getVmsMap(account);
        Map<Long, String> clusterNames = client.getClusterNames();
        Map<Long, Map<String, String>> vmNamesAllClusters = client.getVmNamesAllClusters();

        List<ConsistencyGroupSetSettings> groupSetsSettings = rpSettings.getGroupsSetsSettings();
        Map<String, GroupSet> groupIdToGroupSetMap = getGroupIdToGroupSetMap(groupSetsSettings);

        Map<ConsistencyGroupCopyUID, String> transferStatesMap = getGroupCopiesTransferStates(
        client);
        Map<String, Long> volumesMaxSizesMap = getGroupVolumesMaxSizes(client);

        List<ConsistencyGroupSettings> groupSettingsList = rpSettings.getGroupsSettings();
        for (ConsistencyGroupSettings groupSettings : groupSettingsList) {

            Map<ConsistencyGroupCopyUID, List<CopySnapshot>> copySnapshotsMap = getGroupCopiesSnapshots(
            client, groupSettings.getGroupUID().getId());
            Map<String, ClusterDefinition> handledCustersMap = new HashMap<String, ClusterDefinition>();
            ConsistencyGroup consistencyGroup = new ConsistencyGroup();
            String groupId = new Long(groupSettings.getGroupUID().getId()).toString();
            String groupName = groupSettings.getName();
            consistencyGroup.setId(groupId);
            consistencyGroup.setName(groupName);
            consistencyGroup.setMaxVolumeSize(volumesMaxSizesMap.get(groupId));

            List<VmReplicationSetSettings> vmReplicationSetSettingsList = groupSettings
            .getVmReplicationSetsSettings();

            ClusterDefinition productionCluster = null;
            List<ClusterDefinition> replicaClusters = new LinkedList<ClusterDefinition>();
            consistencyGroup.setReplicaClusters(replicaClusters);
            List<VmDefinition> vmsList = new LinkedList<VmDefinition>();
            consistencyGroup.setVms(vmsList);

            for (VmReplicationSetSettings vmReplicationSet : vmReplicationSetSettingsList) {

                List<VmReplicationSettings> vmReplicationSettingsList = vmReplicationSet
                .getReplicatedVMs();

                for (VmReplicationSettings vmReplication : vmReplicationSettingsList) {
                    String vmId = vmReplication.getVmUID().getUuid();
                    ConsistencyGroupCopyUID copyId = vmReplication.getGroupCopyUID();
                    Long clusterId = copyId.getGlobalCopyUID().getClusterUID().getId();
                    String clusterName = clusterNames.get(clusterId);
                    if (currSystem.getNameToClusterMap().get(clusterName) != null) {
                        clusterName = currSystem.getNameToClusterMap().get(clusterName)
                        .getFriendlyName();
                    }
                    String vmName = vmNamesAllClusters.get(clusterId).get(vmId);
                    List<ConsistencyGroupCopyUID> production = groupSettings
                    .getProductionCopiesUID();
                    //this vm belongs to production
                    if (production.contains(copyId)) {
                        //process vm only if it belongs to account
                        if (vmsMap.get(vmId) != null) {
                            //remove from unprotected candidates
                            vmsMap.remove(vmId);
                            VmDefinition currVm = new VmDefinition(vmId, vmName);
                            vmsList.add(currVm);
                            productionCluster = new ClusterDefinition(clusterId.toString(),
                            clusterName);
                            consistencyGroup.setProductionCluster(productionCluster);
                        }
                    }
                    //this vm belongs to replica
                    else {
                        ClusterDefinition replicaCluster = null;
                        if (handledCustersMap.get(clusterId.toString()) != null) {
                            replicaCluster = handledCustersMap.get(clusterId.toString());
                        } else {
                            replicaCluster = new ClusterDefinition(clusterId.toString(),
                            clusterName);
                            handledCustersMap.put(clusterId.toString(), replicaCluster);
                            replicaClusters.add(replicaCluster);
                        }
                        GroupCopySettings groupCopySettings = getGroupCopySettings(copyId,
                        groupSettings, transferStatesMap, copySnapshotsMap);
                        //add the copy in case it wasn't added in context of another vm
                        if (!replicaCluster.isExistingCopy(groupCopySettings)) {
                            replicaCluster.addGroupCopy(groupCopySettings);
                        }

                    }

                }

            }

            if (!consistencyGroup.getVms().isEmpty()) {
                if (groupIdToGroupSetMap.get(consistencyGroup.getId()) != null) {
                    GroupSet groupSet = groupIdToGroupSetMap.get(consistencyGroup.getId());
                    groupSet.addConsistencyGroup(consistencyGroup);
                } else {
                    protectedVms.add(consistencyGroup);
                }

            }
        }
        List<VmContainer> groupSets = getNotEmptyUniqueGroupSets(groupIdToGroupSetMap);
        accountVmsStructure.getProtectedVms().addAll(groupSets);
        accountVmsStructure.getProtectedVms().addAll(protectedVms);
        List<VmDefinition> unprotectedVms = getVmDefinitionsList(vmsMap);
        accountVmsStructure.setUnprotectedVms(unprotectedVms);
        return accountVmsStructure;
    }

    private Map<String, VmOwnership> getVmsMap(Account account) {
        List<VmOwnership> vms = account.getVms();
        Map<String, VmOwnership> vmsMap = vms.stream()
        .collect(Collectors.toMap(VmOwnership::getVmId, (p) -> p));
        return vmsMap;
    }

    private List<VmDefinition> getVmDefinitionsList(Map<String, VmOwnership> vmsMap) {
        List<VmDefinition> vmsList = new LinkedList<VmDefinition>();
        if (vmsMap != null && !vmsMap.isEmpty()) {
            for (String vmId : vmsMap.keySet()) {
                VmDefinition vmDefinition = new VmDefinition(vmId, vmsMap.get(vmId).getVmName());
                vmsList.add(vmDefinition);
            }
        }
        return vmsList;
    }

    private Map<String, GroupSet> getGroupIdToGroupSetMap(
    List<ConsistencyGroupSetSettings> groupSetSettings) {
        Map<String, GroupSet> cgIdToGs = new HashMap<String, GroupSet>();
        if (groupSetSettings != null) {
            for (ConsistencyGroupSetSettings currGroupSetSettings : groupSetSettings) {
                GroupSet currGroupSet = new GroupSet();
                Long setUID = currGroupSetSettings.getSetUID().getId();
                currGroupSet.setId(setUID.toString());
                currGroupSet.setName(currGroupSetSettings.getName());
                for (ConsistencyGroupUID currConsistencyGroupUID : currGroupSetSettings
                .getGroupsUIDs()) {
                    Long groupUID = currConsistencyGroupUID.getId();
                    cgIdToGs.put(groupUID.toString(), currGroupSet);
                }
            }
        }
        return cgIdToGs;
    }

    private List<VmContainer> getNotEmptyUniqueGroupSets(Map<String, GroupSet> groupSets) {
        List<VmContainer> notEmptyGroupsSets = new LinkedList<VmContainer>();
        Map<String, GroupSet> visited = new HashMap<String, GroupSet>();
        for (Entry<String, GroupSet> entry : groupSets.entrySet()) {
            if (visited.get(entry.getValue().getId()) == null) {
                visited.put(entry.getValue().getId(), entry.getValue());
                if (!entry.getValue().getConsistencyGroups().isEmpty()) {
                    notEmptyGroupsSets.add(entry.getValue());
                }
            }

        }
        return notEmptyGroupsSets;
    }

    private GroupCopySettings getGroupCopySettings(ConsistencyGroupCopyUID copyId,
    ConsistencyGroupSettings consistencyGroupSettings,
    Map<ConsistencyGroupCopyUID, String> transferStatesMap,
    Map<ConsistencyGroupCopyUID, List<CopySnapshot>> copySnapshotsMap) {

        GroupCopySettings groupCopySettings = null;
        List<ConsistencyGroupCopySettings> allCopiesSettings = consistencyGroupSettings
        .getGroupCopiesSettings();
        for (ConsistencyGroupCopySettings currGroupCopySettings : allCopiesSettings) {
            if (currGroupCopySettings.getCopyUID().equals(copyId)) {
                Long imageAccessTimeStamp = 0L;
                groupCopySettings = new GroupCopySettings();
                groupCopySettings
                .setId(new Integer(copyId.getGlobalCopyUID().getCopyUID()).toString());
                groupCopySettings.setName(currGroupCopySettings.getName());
                groupCopySettings.setClusterId(new Long(
                currGroupCopySettings.getCopyUID().getGlobalCopyUID().getClusterUID().getId())
                .toString());
                if (currGroupCopySettings.getImageAccessInformation() != null
                && currGroupCopySettings.getImageAccessInformation().isImageAccessEnabled()) {
                    groupCopySettings.setImageAccess(ImageAccess.ENABLED.value());
                    imageAccessTimeStamp = currGroupCopySettings.
                    getImageAccessInformation().
                    getImageInformation().
                    getTimeStamp().getTimeInMicroSeconds();
                } else {
                    groupCopySettings.setImageAccess(ImageAccess.DISABLED.value());
                }
                groupCopySettings
                .setReplication(transferStatesMap.get(currGroupCopySettings.getCopyUID()));
                List<CopySnapshot> allSnapshots = copySnapshotsMap
                .get(currGroupCopySettings.getCopyUID());
                if (groupCopySettings.getImageAccess().equals(ImageAccess.ENABLED.value())) {
                    setSnapshotImageAccess(allSnapshots, imageAccessTimeStamp);
                }
                groupCopySettings.setSnapshots(getSnapshotsByType(allSnapshots, false));
                groupCopySettings.setBookmarks(getSnapshotsByType(allSnapshots, true));
            }
        }

        return groupCopySettings;
    }

    private void setSnapshotImageAccess(List<CopySnapshot> allSnapshots,
    Long imageAccessTimeStamp) {
        if (allSnapshots != null) {
            for (CopySnapshot currCopySnapshot : allSnapshots) {
                if (currCopySnapshot.getOriginalClosingTimeStamp().equals(imageAccessTimeStamp)) {
                    currCopySnapshot.setImageAccessEnabled(true);
                }
            }
        }
    }

    private Map<ConsistencyGroupCopyUID, String> getGroupCopiesTransferStates(Client client) {
        Map<ConsistencyGroupCopyUID, String> statesMap = new HashMap<ConsistencyGroupCopyUID, String>();
        ConsistencyGroupStateSet consistencyGroupStateSet = client.getConsistencyGroupStateSet();
        List<ConsistencyGroupState> groupsStates = consistencyGroupStateSet.getInnerSet();

        for (ConsistencyGroupState currGroupState : groupsStates) {
            List<ConsistencyGroupLinkState> linksStates = currGroupState.getLinksState();
            for (ConsistencyGroupLinkState consistencyGroupLinkState : linksStates) {
                ConsistencyGroupUID consistencyGroupUID = consistencyGroupLinkState
                .getGroupLinkUID().getGroupUID();
                GlobalCopyUID globalCopyUID = consistencyGroupLinkState.getGroupLinkUID()
                .getSecondCopy();
                ConsistencyGroupCopyUID consistencyGroupCopyUID = new ConsistencyGroupCopyUID(
                consistencyGroupUID, globalCopyUID);
                PipeState pipeState = consistencyGroupLinkState.getPipeState();
                switch (pipeState) {
                case INITIALIZING:
                    statesMap.put(consistencyGroupCopyUID, TransferState.INITIALIZING.value());
                    break;
                case ACTIVE:
                    statesMap.put(consistencyGroupCopyUID, TransferState.ACTIVE.value());
                    break;
                case PAUSED:
                    statesMap.put(consistencyGroupCopyUID, TransferState.PAUSED.value());
                    break;
                case ERROR:
                    statesMap.put(consistencyGroupCopyUID, TransferState.ERROR.value());
                    break;
                case UNKNOWN:
                    statesMap.put(consistencyGroupCopyUID, TransferState.UNKNOWN.value());
                    break;
                default:
                    statesMap.put(consistencyGroupCopyUID, TransferState.UNKNOWN.value());
                    break;
                }
            }
        }

        return statesMap;

    }

    private Map<String, Long> getGroupVolumesMaxSizes(Client client) {
        Map<String, Long> volumesMaxSizes = new HashMap<String, Long>();
        ConsistencyGroupVolumesStateSet volumesStateSet = client
        .getConsistencyGroupVolumesStateSet();
        List<ConsistencyGroupVolumesState> volumesStates = volumesStateSet.getInnerSet();
        for (ConsistencyGroupVolumesState consistencyGroupVolumesState : volumesStates) {
            Long groupId = consistencyGroupVolumesState.getGroupUID().getId();
            List<ReplicationSetVolumesState> replicationSetVolumesState = consistencyGroupVolumesState
            .getReplicationSetsVolumesState();
            long totalVolumeSize = 0;
            for (ReplicationSetVolumesState volumeState : replicationSetVolumesState) {
                totalVolumeSize += volumeState.getMaxPossibleSizeInBytes();
            }
            Long totalVolumeSizeGB = totalVolumeSize / 1000 / 1000 / 1000;
            volumesMaxSizes.put(groupId.toString(), totalVolumeSizeGB);
        }
        return volumesMaxSizes;
    }

    private Map<ConsistencyGroupCopyUID, List<CopySnapshot>> getGroupCopiesSnapshots(Client client,
    Long groupId) {
        Map<ConsistencyGroupCopyUID, List<CopySnapshot>> copyUIDToSnapshotsMap = new HashMap<ConsistencyGroupCopyUID, List<CopySnapshot>>();
        ConsistencyGroupSnapshots consistencyGroupSnapshots = client.getGroupSnapshots(groupId);
        List<ConsistencyGroupCopySnapshots> copiesSnapshots = consistencyGroupSnapshots
        .getCopiesSnapshots();

        for (ConsistencyGroupCopySnapshots currCopy : copiesSnapshots) {
            ConsistencyGroupCopyUID copyUID = currCopy.getCopyUID();
            List<Snapshot> snapshots = currCopy.getSnapshots();
            List<CopySnapshot> snapshotsList = new LinkedList<CopySnapshot>();
            int maxSnapshots = 50;
            int currSnapshotCounter = 0;
            for (Snapshot currSnapshot : snapshots) {
                if (currSnapshotCounter < maxSnapshots || (currSnapshot.getRelevantEvent() != null
                && !StringUtils.isEmpty(currSnapshot.getRelevantEvent().getDetails()))) {
                    RecoverPointTimeStamp timestamp = currSnapshot.getClosingTimeStamp();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                    String dateStr = dateFormat
                    .format(new Date(timestamp.getTimeInMicroSeconds() / 1000));
                    SnapshotUID snapshotUID = currSnapshot.getSnapshotUID();
                    CopySnapshot snapshot = new CopySnapshot();
                    snapshot.setId(snapshotUID.getId());
                    snapshot.setClosingTimestamp(dateStr);
                    snapshot.setOriginalClosingTimeStamp(timestamp.getTimeInMicroSeconds());
                    if (currSnapshot.getRelevantEvent() != null) {
                        snapshot.setName(currSnapshot.getRelevantEvent().getDetails());
                    }
                    snapshotsList.add(snapshot);
                }
                currSnapshotCounter++;
            }
            copyUIDToSnapshotsMap.put(copyUID, snapshotsList);
        }
        return copyUIDToSnapshotsMap;

    }

    private List<CopySnapshot> getSnapshotsByType(List<CopySnapshot> snapshots,
    boolean isBookmark) {
        List<CopySnapshot> res = new LinkedList<CopySnapshot>();
        if (snapshots != null) {
            for (CopySnapshot currSnapshot : snapshots) {
                if (isBookmark) {
                    if (!StringUtils.isEmpty(currSnapshot.getName())) {
                        res.add(currSnapshot);
                    }
                } else {
                    if (StringUtils.isEmpty(currSnapshot.getName())) {
                        res.add(currSnapshot);
                    }
                }
            }
        }
        return res;
    }

}
