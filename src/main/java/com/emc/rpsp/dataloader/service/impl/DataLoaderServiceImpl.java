package com.emc.rpsp.dataloader.service.impl;

import com.emc.rpsp.RpspException;
import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.accounts.service.AccountService;
import com.emc.rpsp.dataloader.domain.InternalData;
import com.emc.rpsp.dataloader.service.DataLoaderService;
import com.emc.rpsp.fal.Client;
import com.emc.rpsp.packages.domain.PackageDefinition;
import com.emc.rpsp.packages.service.PackageDefinitionService;
import com.emc.rpsp.rpsystems.ClusterSettings;
import com.emc.rpsp.rpsystems.SystemConnectionInfoRepository;
import com.emc.rpsp.rpsystems.SystemSettings;
import com.emc.rpsp.users.domain.User;
import com.emc.rpsp.users.service.UserService;
import com.emc.rpsp.virtualconfig.domain.VcenterConfig;
import com.emc.rpsp.virtualconfig.service.VirtualConfigurationService;
import com.emc.rpsp.vms.domain.VmOwnership;
import com.emc.rpsp.vms.service.VmOwnershipService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DataLoaderServiceImpl implements DataLoaderService {

    private final String CLASSPATH_INTERNAL_DATA_EXPRESSION = "classpath:data-loader/internal-data.template";
    private final Logger log = LoggerFactory.getLogger(DataLoaderServiceImpl.class);
    @Autowired
    private SystemConnectionInfoRepository systemConnectionInfoRepository;
    @Autowired
    private PackageDefinitionService packageDefService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private VmOwnershipService vmOwnershipService;
    @Autowired
    private UserService userService;
    @Autowired
    private VirtualConfigurationService virtualConfigurationService;
    @Autowired
    private ResourcePatternResolver resourceLoader = null;

    @Override
    @Transactional("transactionManager")
    public InternalData getInternalData(Boolean includeVirtualConfig) {
        InternalData internalData = new InternalData();
        internalData.setSystems(systemConnectionInfoRepository.findAll());
        internalData.setPackages(packageDefService.findAll());
        internalData.setTenants(accountService.findAll());
        internalData.setUsers(userService.findUsers());
        internalData.setVms(vmOwnershipService.findAll());
        setPackageClustersData(internalData);
        handleClustersFriendlyNames(internalData);
        if (includeVirtualConfig) {
            addVirtualConfigurationInfo(internalData.getSystems());
        }
        return internalData;
    }


    @Override
    @Transactional("transactionManager")
    public InternalData populateInternalData(InternalData internalData) {
        //load existing users
        List<User> existingUsers = userService.findUsers();
        Map<String, User> existingUsersMap = new HashMap<String, User>();
        if (existingUsers != null) {
            existingUsersMap = existingUsers.stream()
                .collect(Collectors.toMap(User::getLogin, u -> u));
        }

        //clean the configuration
        systemConnectionInfoRepository.deleteAll();


        //systems
        List<SystemSettings> systems = internalData.getSystems();
        Map<String, SystemSettings> systemsMap = systems.stream().collect(Collectors.toMap(SystemSettings::getName, (p) -> p));
        for (SystemSettings systemSettings : systems) {
            propagateClusterData(systemSettings);
            systemSettings.setId(null);
        }

        //packages
        List<PackageDefinition> packages = internalData.getPackages();
        Map<String, PackageDefinition> packagesMap = packages.stream().collect(Collectors.toMap(PackageDefinition::getName, (p) -> p));
        for (PackageDefinition packageDefinition : packages) {
            SystemSettings currSystem = systemsMap.get(packageDefinition.getSystemName());
            packageDefinition.setSystemSettings(currSystem);
            currSystem.addPackage(packageDefinition);

            if (packageDefinition.getSourceClusterIdStr() != null) {
                packageDefinition.setSourceClusterId(Long.parseLong(packageDefinition.getSourceClusterIdStr()));
            }

            if (packageDefinition.getTargetClusterIdStr() != null) {
                packageDefinition.setTargetClusterId(Long.parseLong(packageDefinition.getTargetClusterIdStr()));
            }

            packageDefinition.setId(null);
        }

        //accounts
        List<Account> tenants = internalData.getTenants();
        Map<String, Account> accountsMap = tenants.stream().collect(Collectors.toMap(Account::getName, (p) -> p));
        for (Account tenant : tenants) {
            for (String packageName : tenant.getPackageNames()) {
                PackageDefinition currPackage = packagesMap.get(packageName);
                tenant.addPackage(currPackage);
                currPackage.addAccount(tenant);
            }
            tenant.setId(null);
        }

        //users
        List<User> users = internalData.getUsers();
        for (User user : users) {
            //existing user user - no password is passed
            if (StringUtils.isEmpty(user.getPassword())) {
                User existingUser = existingUsersMap.get(user.getLogin());
                if (existingUser == null) {
                    throw new RpspException("No password for user " + user.getLogin());
                }
                user.setPassword(existingUser.getPassword());
            } else {
                user.setEncodedPassword(user.getPassword());
            }
            user.setCreatedBy("admin");
            user.setCreatedDate(new DateTime());
            user.setPermission("USER");

            Account tenant = accountsMap.get(user.getTenantName());

            //regular user - not admin
            if (tenant != null) {
                user.setAccount(tenant);
                tenant.addUser(user);
            }

            user.setId(null);
        }


        //vms
        List<VmOwnership> vms = internalData.getVms();
        if (vms != null) {
            for (VmOwnership vmOwnership : vms) {
                Account tenant = accountsMap.get(vmOwnership.getTenantName());
                vmOwnership.setAccount(tenant);
                tenant.addVm(vmOwnership);
            }
        }

        //save new configuration
        for (SystemSettings systemSettings : systems) {
            systemConnectionInfoRepository.saveAndFlush(systemSettings);
        }
        InternalData res = getInternalData(true);
        return res;
    }

    @Override
    public String getInternalDataTemplate() {
        String template = null;
        InputStream is = null;
        try {
            Resource resource = resourceLoader
                .getResource(CLASSPATH_INTERNAL_DATA_EXPRESSION);
            is = resource.getInputStream();
            template = IOUtils.toString(is, "UTF-8");
        } catch (Exception e) {
            throw new RpspException("Couldn't read internal data configuration");
        } finally {
            IOUtils.closeQuietly(is);
        }
        return template;
    }


    @Override
    public void deleteInternalData() {
        //clean the configuration
        systemConnectionInfoRepository.deleteAll();

    }

    private void addVirtualConfigurationInfo(List<SystemSettings> systemSettingsList) {
        for (SystemSettings systemSettings : systemSettingsList) {
            addVirtualConfigurationInfo(systemSettings);
        }
    }

    private void addVirtualConfigurationInfo(SystemSettings systemSettings) {
        List<ClusterSettings> clusters = systemSettings.getClusters();
        for (ClusterSettings cluster : clusters) {
            VcenterConfig vcenterConfig = virtualConfigurationService.getVirtualConfiguration(cluster.getClusterId());
            cluster.setVcenterConfig(vcenterConfig);
        }
    }

    private void propagateClusterData(SystemSettings systemSettings) {
        Map<String, Object> clusterFriendlyNames = new HashMap<String, Object>();
        List<ClusterSettings> clustersParams = systemSettings.getClusters();
        for (ClusterSettings currClusterSettings : clustersParams) {
            clusterFriendlyNames.put(currClusterSettings.getClusterName(), currClusterSettings.getFriendlyName());
        }
        systemSettings.setClusters(new LinkedList<>());
        Client client = new Client(systemSettings, systemConnectionInfoRepository);
        try {
            client.getSystemTimeStateless();
        } catch (Exception e) {
            throw new RpspException("Couldn't access system " + systemSettings.getSystemIp());
        }
        Map<Long, String> clusters = client.getClusterNames();
        for (Map.Entry<Long, String> entry : clusters.entrySet()) {
            ClusterSettings cluster = new ClusterSettings(entry.getKey(), entry.getValue(),
                systemSettings);

            Object clusterFriendlyName = clusterFriendlyNames.get(entry.getValue());
            if (null != clusterFriendlyName) {
                cluster.setFriendlyName(clusterFriendlyName.toString());
                systemSettings.addCluster(cluster);
            }

        }
    }


    private void setPackageClustersData(InternalData internalData) {
        List<SystemSettings> systems = internalData.getSystems();
        List<PackageDefinition> packages = internalData.getPackages();

        for (PackageDefinition currPackage : packages) {
            for (SystemSettings currSystem : systems) {
                for (ClusterSettings currCluster : currSystem.getClusters()) {
                    if (currPackage.getSourceClusterId().equals(currCluster.getClusterId())) {
                        currPackage.setSourceClusterName(currCluster.getFriendlyName());
                    }
                    if (currPackage.getTargetClusterId().equals(currCluster.getClusterId())) {
                        currPackage.setTargetClusterName(currCluster.getFriendlyName());
                    }
                }
            }
        }
    }
    
    
    private void handleClustersFriendlyNames(InternalData internalData) {
     		 
        internalData.getSystems().stream()
    	.forEach(s -> s.getClusters().stream()
    		 .filter(c -> StringUtils.isEmpty(c.getFriendlyName()))
    			 .forEach(cl -> cl.setFriendlyName(cl.getClusterName())));
        
    }


}