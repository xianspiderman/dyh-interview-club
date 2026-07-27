package com.dyh.club.auth.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限dto
 * 
 */
@Data
public class AuthPermissionBO implements Serializable {

    private Long id;
    
    private String name;
    
    private Long parentId;
    
    private Integer type;
    
    private String menuUrl;
    
    private Integer status;
    
    private Integer show;
    
    private String icon;
    
    private String permissionKey;
}

