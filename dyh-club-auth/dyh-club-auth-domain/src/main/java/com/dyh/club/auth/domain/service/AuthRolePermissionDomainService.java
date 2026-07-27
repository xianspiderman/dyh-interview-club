package com.dyh.club.auth.domain.service;

import com.dyh.club.auth.domain.entity.AuthPermissionBO;
import com.dyh.club.auth.domain.entity.AuthRolePermissionBO;

/**
 * 角色领域service
 * 
 */
public interface AuthRolePermissionDomainService {

    Boolean add(AuthRolePermissionBO authRolePermissionBO);

}
