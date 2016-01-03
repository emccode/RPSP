package com.emc.rpsp.backupsystems;

import com.emc.rpsp.vmwal.VSphereApi;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by morand3 on 1/3/2016.
 */
@Service
public class BackupApi {
    private final Logger log = LoggerFactory.getLogger(BackupApi.class);
    @Autowired
    private BackupSystemsRepository repository;

    public List<String> getBackupsList(BackupSystem system) {
        VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
        List<String> vms = vSphereApi.vmsInFolder(system.getBackupFolder());
        return vms;
    }

    public List<String> getBackupsList(String vmName) {
        VmBackup vm = repository.findVmByName(vmName);
        List<String> vms = getBackupsList(vm.getBackupSystem());
        return fetchVmBackupsList(vms, vmName);
    }

    public List<String> fetchVmBackupsList(List<String> backups, String vmName) {
        List<String> res = new LinkedList<>();
        for (String backup : backups) {
            if (backup.startsWith(vmName))
                res.add(backup);
        }
        return res;
    }

    public void backupVm(String vmName) {
        VmBackup vm = repository.findVmByName(vmName);
        String vmDrTestName = getDrTestVmName(vmName);
        BackupSystem system = vm.getBackupSystem();
        VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
        try {
            vSphereApi.cloneVM(vmDrTestName, system.getBackupFolder(), currentBackupName(vmName), system.getBackupDatastore(), false);
        } catch (Exception e) {
            log.warn("Failed to backup/clone VM {}", vmName);
        }
    }

    public void enableAccessBackup(BackupSystem system, String backupName) {
        if (null == system || null == backupName) {
            log.warn("Called enable access backup with no params");
        }
        try {
            VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
            vSphereApi.cloneVM(backupName,
                system.getAccessBackupFolder(),
                accessBackupName(backupName),
                system.getAccessBackupDatastore(),
                true);
        } catch (Exception e) {
            log.warn("Failed to backup/clone VM {}", backupName);
        }
    }

    private String getDrTestVmName(String vmName) {
        //TODO Boris - please implement
        return vmName;
    }

    private String accessBackupName(String backup) {
        return backup + "_restore";
    }

    private String currentBackupName(String vmName) {
        return vmName + DateFormatUtils.format(new Date(), "_yyyy-MM-dd");
    }

}