package com.emc.rpsp.accounts.domain;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.emc.rpsp.rpsystems.SystemSettings;
import com.emc.rpsp.users.domain.User;
import com.emc.rpsp.vms.domain.VmOwnership;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "T_ACCOUNT")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Account implements Serializable {

	@Id
	@GeneratedValue
	private Long id;


	@Size(min = 0, max = 100)
	@Column(name = "name", length = 100)
	private String name;
	
	
	@Size(min = 0, max = 100)
	@Column(name = "label", length = 100)
	private String label;
	
	@JsonIgnore
	@Column
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<VmOwnership> vms;
	
	@JsonIgnore
	@Column
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<User> users;

	@JsonIgnore
	@JoinTable(name="T_ACCOUNT_SYSTEMS")
	@ManyToMany(targetEntity = com.emc.rpsp.rpsystems.SystemSettings.class, fetch = FetchType.LAZY)
	private List<SystemSettings> systemSettings;	
	
	
	
	public Account() {
		super();
		vms = new LinkedList<>();
		users = new LinkedList<>();	
		systemSettings = new LinkedList<>();
	}

	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<VmOwnership> getVms() {
		return vms;
	}

	public void setVms(List<VmOwnership> vms) {
		this.vms = vms;
	}

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}	
	
	
	public List<SystemSettings> getSystemSettings() {
		return systemSettings;
	}



	public void setSystemSettings(List<SystemSettings> systemSettings) {
		this.systemSettings = systemSettings;
	}
	
	public void addVm(VmOwnership vmOwnership){
		vms.add(vmOwnership);
	}
	
	public void addUser(User user){
		users.add(user);
	}
	
	public void addSystem(SystemSettings systemSettings){
		this.systemSettings.add(systemSettings);
	}

	
	@Override
	public String toString() {
		return "Account{" + "id='" + id + '\'' + ", name='" + name
		        + '\'' + ", label='" + label + "}";
	}
}