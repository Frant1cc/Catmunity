package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.*;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.UserAdminVO;
import org.catmunity.model.vo.UserAuthVO;
import org.catmunity.model.vo.UserLoginVO;
import org.catmunity.result.PageResult;

import java.util.List;

public interface UserService extends IService<User> {

    UserLoginVO login(UserLoginDTO userLoginDTO);

    UserAuthVO authUser(UserAuthDTO userAuthDTO);

    void logout(String token);

    PageResult<UserAdminVO> getAdminUserPage(UserQueryDTO queryDTO, Integer pageNum, Integer pageSize);

    UserAdminVO getUserAdminDetail(Long userId);

    void banUser(Long userId, String reason);

    void unbanUser(Long userId);

    void assignUserRole(Long userId, Integer userType);

    void batchBanUsers(List<Long> userIds);

    void updateUserInfo(Long userId, UserUpdateDTO updateDTO);

    boolean sendRegisterEmailCode(String email);

    UserLoginVO register(UserRegisterDTO registerDTO);
}