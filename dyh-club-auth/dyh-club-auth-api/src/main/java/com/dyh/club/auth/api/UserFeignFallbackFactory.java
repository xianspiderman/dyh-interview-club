package com.dyh.club.auth.api;

import com.dyh.club.auth.entity.AuthUserDTO;
import com.dyh.club.auth.entity.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignService> {
    @Override public UserFeignService create(Throwable cause) {
        return new UserFeignService() {
            @Override public Result<AuthUserDTO> getUserInfo(AuthUserDTO request) { return Result.fail(); }
            @Override public Result<List<AuthUserDTO>> listUserInfoByIds(List<String> names) { return Result.fail(); }
        };
    }
}
