package com.dyh.club.auth.domain.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.gson.Gson;
import com.dyh.club.auth.common.enums.AuthUserStatusEnum;
import com.dyh.club.auth.common.enums.IsDeletedFlagEnum;
import com.dyh.club.auth.domain.constants.AuthConstant;
import com.dyh.club.auth.domain.convert.AuthUserBOConverter;
import com.dyh.club.auth.domain.entity.AuthUserBO;
import com.dyh.club.auth.domain.redis.RedisUtil;
import com.dyh.club.auth.domain.service.AuthUserDomainService;
import com.dyh.club.auth.infra.basic.entity.AuthPermission;
import com.dyh.club.auth.infra.basic.entity.AuthRole;
import com.dyh.club.auth.infra.basic.entity.AuthRolePermission;
import com.dyh.club.auth.infra.basic.entity.AuthUser;
import com.dyh.club.auth.infra.basic.entity.AuthUserRole;
import com.dyh.club.auth.infra.basic.service.AuthPermissionService;
import com.dyh.club.auth.infra.basic.service.AuthRolePermissionService;
import com.dyh.club.auth.infra.basic.service.AuthRoleService;
import com.dyh.club.auth.infra.basic.service.AuthUserRoleService;
import com.dyh.club.auth.infra.basic.service.AuthUserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthUserDomainServiceImpl implements AuthUserDomainService {

    @Resource
    private AuthUserService authUserService;

    @Resource
    private AuthUserRoleService authUserRoleService;

    @Resource
    private AuthPermissionService authPermissionService;

    @Resource
    private AuthRolePermissionService authRolePermissionService;

    @Resource
    private AuthRoleService authRoleService;

    @Value("${dyh.auth.password-salt}")
    private String salt;

    @Value("${dyh.user.default-avatar-url:}")
    private String defaultAvatarUrl;

    @Resource
    private RedisUtil redisUtil;

    private String authPermissionPrefix = "auth.permission";

    private String authRolePrefix = "auth.role";

    private static final String LOGIN_PREFIX = "loginCode";

    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(AuthUserBO authUserBO) {

        AuthUser existAuthUser = new AuthUser();
        existAuthUser.setUserName(authUserBO.getUserName());
        List<AuthUser> existUser = authUserService.queryByCondition(existAuthUser);
        if (existUser.size() > 0) {
            return true;
        }
        AuthUser authUser = AuthUserBOConverter.INSTANCE.convertBOToEntity(authUserBO);
        if (StringUtils.isNotBlank(authUser.getPassword())) {//有密码再加密，下面的微信登录doLogin是不输入密码的
            authUser.setPassword(SaSecureUtil.md5BySalt(authUser.getPassword(), salt));
        }
        if (StringUtils.isBlank(authUser.getAvatar()) && StringUtils.isNotBlank(defaultAvatarUrl)) {
            authUser.setAvatar(defaultAvatarUrl);
        }
        if (StringUtils.isBlank(authUser.getNickName())) {
            authUser.setNickName("DYH Club 用户");
        }
        authUser.setStatus(AuthUserStatusEnum.OPEN.getCode());
        authUser.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        Integer count = authUserService.insert(authUser);


        // 新用户默认关联普通用户角色，并预热角色与权限缓存。
        AuthRole authRole = new AuthRole();
        authRole.setRoleKey(AuthConstant.NORMAL_USER);//得到auth_role表里的角色的key
        AuthRole roleResult = authRoleService.queryByCondition(authRole);
        Long roleId = roleResult.getId();//得到角色 ID
        Long userId = authUser.getId();//得到用户ID
        AuthUserRole authUserRole = new AuthUserRole();//下面开始利用桥接表（中间表）建立物理关联
        authUserRole.setUserId(userId);
        authUserRole.setRoleId(roleId);
        authUserRole.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        authUserRoleService.insert(authUserRole);

        // 提前预热（缓存）权限数据，利用共享内存实现鉴权的高效解耦，这段代码的本质是：Auth服务在用户刚注册完（或登录完）的那一瞬间，趁着自己刚查完数据库，顺手把这个用户的身份信息写在了“公共黑板”上。这样，网关保安以后查票时，直接看黑板就行了，根本不需要再来跑来打扰 Auth 服务。
        String roleKey = redisUtil.buildKey(authRolePrefix, authUser.getUserName());// 1. 制造一把只有网关和Auth都知道的“通用钥匙”
        List<AuthRole> roleList = new LinkedList<>();// 2. 把角色打包
        roleList.add(authRole);// authRole是上面刚查出来的“普通用户”角色
        redisUtil.set(roleKey, new Gson().toJson(roleList));// 3. 序列化并存入 Redis，此时 Redis 里多了一条数据 -> Key: "auth:role:张三", Value: "[{id:1, roleKey:'user'...}]"
        // 下面缓存权限数据，光知道张三是“普通用户”不够，网关还需要知道“普通用户”到底能点哪些按钮。所以 Auth 必须帮网关把具体的权限也查出来贴在黑板上。
        AuthRolePermission authRolePermission = new AuthRolePermission();// 1. 拿着 roleId 去查【角色-权限】的中间表
        authRolePermission.setRoleId(roleId);
        List<AuthRolePermission> rolePermissionList = authRolePermissionService.
                queryByCondition(authRolePermission);

        List<Long> permissionIdList = rolePermissionList.stream()
                .map(AuthRolePermission::getPermissionId).collect(Collectors.toList());// 2. 把中间表里的 permissionId 提取出来，组成一个纯粹的 ID 集合 (List<Long>)
        //根据roleId查权限 // 3. 拿着这一堆 permissionId，去查最终的权限实体（比如“新增题目”、“查看列表”）
        List<AuthPermission> permissionList = authPermissionService.queryByRoleList(permissionIdList);
        String permissionKey = redisUtil.buildKey(authPermissionPrefix, authUser.getUserName());// 4. 制造第二把钥匙，存入 Redis，同样地，拼接出 auth:permission:张三 的 Key，然后把具体的菜单权限用 JSON 格式存入 Redis。
        redisUtil.set(permissionKey, new Gson().toJson(permissionList));

        return count > 0;
    }

    @Override
    public Boolean update(AuthUserBO authUserBO) {
        AuthUser authUser = AuthUserBOConverter.INSTANCE.convertBOToEntity(authUserBO);
        Integer count = authUserService.updateByUserName(authUser);
        return count > 0;
    }

    @Override
    public Boolean delete(AuthUserBO authUserBO) {
        AuthUser authUser = new AuthUser();
        authUser.setId(authUserBO.getId());
        authUser.setIsDeleted(IsDeletedFlagEnum.DELETED.getCode());
        Integer count = authUserService.update(authUser);
        //有任何的更新，都要与缓存进行同步的修改
        return count > 0;
    }

    @Override
    public SaTokenInfo doLogin(String validCode) {
        String loginKey = redisUtil.buildKey(LOGIN_PREFIX, validCode);
        String openId = redisUtil.get(loginKey);
        if (StringUtils.isBlank(openId)) {
            return null;
        }
        AuthUserBO authUserBO = new AuthUserBO();
        authUserBO.setUserName(openId);
        this.register(authUserBO);
        StpUtil.login(openId);
        return StpUtil.getTokenInfo();
    }

    @Override
    public AuthUserBO getUserInfo(AuthUserBO authUserBO) {
        AuthUser authUser = new AuthUser();
        authUser.setUserName(authUserBO.getUserName());
        List<AuthUser> userList = authUserService.queryByCondition(authUser);
        if (CollectionUtils.isEmpty(userList)) {
            return new AuthUserBO();
        }
        AuthUser user = userList.get(0);
        return AuthUserBOConverter.INSTANCE.convertEntityToBO(user);
    }

    @Override
    public List<AuthUserBO> listUserInfoByIds(List<String> userNameList) {
        List<AuthUser> userList = authUserService.listUserInfoByIds(userNameList);
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return AuthUserBOConverter.INSTANCE.convertEntityToBO(userList);
    }

}
