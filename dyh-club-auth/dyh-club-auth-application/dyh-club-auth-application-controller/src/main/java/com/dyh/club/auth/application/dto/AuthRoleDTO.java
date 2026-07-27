package com.dyh.club.auth.application.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色dto
 *
 */
@Data
public class AuthRoleDTO implements Serializable {

    private Long id;
    
    private String roleName;
    
    private String roleKey;

}

