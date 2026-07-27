package com.dyh.club.auth.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用戶信息dto
 *
 */
@Data
public class AuthUserBO implements Serializable {

    private Long id;

    private String userName;

    private String nickName;

    private String email;

    private String phone;

    private String password;

    private Integer sex;

    private String avatar;

    private Integer status;

    private String introduce;

    private String extJson;

}

