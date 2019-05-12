package com.zhou.ad.service;

import com.zhou.ad.exception.AdException;
import com.zhou.ad.vo.CreateUserRequest;
import com.zhou.ad.vo.CreateUserResponse;

public interface IUserService {

    /**
     * <h2>创建用户</h2>
     * */
    CreateUserResponse createUser(CreateUserRequest request)
            throws AdException;
}
