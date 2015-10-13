package com.emc.rpsp.infra.common.accounts.service;

import java.util.List;

import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.accounts.domain.AccountConfig;

public interface AccountsDataService {
	public List<Account> findAll();
	public List<AccountConfig> findAccountConfigsByAccount(Account account);
}