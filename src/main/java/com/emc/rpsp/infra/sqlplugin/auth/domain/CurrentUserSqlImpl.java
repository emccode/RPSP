package com.emc.rpsp.infra.sqlplugin.auth.domain;

import java.util.List;

import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.infra.common.auth.domain.AbstractCurrentUser;
import com.emc.rpsp.packages.domain.PackageConfig;
import com.emc.rpsp.rpsystems.SystemSettings;
import com.emc.rpsp.users.domain.User;
import com.emc.rpsp.vms.domain.VmOwnership;

import org.springframework.security.core.authority.AuthorityUtils;

public class CurrentUserSqlImpl extends AbstractCurrentUser {

    private User user;

    public CurrentUserSqlImpl(User user) {
        super(user.getLogin(), user.getPassword(),
        AuthorityUtils.createAuthorityList(user.getPermission()));
        this.user = user;

    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

	@Override
	public Account getAccount() {
		return user.getAccount();
	}

	/*@Override
	public List<SystemSettings> getSystemSettings() {
		List<SystemSettings> res = null;
		if(getAccount() != null){
			res = getAccount().getSystemSettings();
		}
		return res;
	}

	@Override
	public List<VmOwnership> getVms() {
		List<VmOwnership> res = null;
		if(getAccount() != null){
			res = getAccount().getVms();
		}
		return res;
	}

	@Override
	public List<AccountConfig> getAccountConfig() {
		List<AccountConfig> res = null;
		if(getAccount() != null){
			res = getAccount().getAccountConfigs();
		}
		return res;
	}*/

}
