package com.dyh.club.auth.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色bo
 *
 */
@Data
public class AuthRoleBO implements Serializable {

    private Long id;
    
    private String roleName;
    
    private String roleKey;

}

