package org.catmunity.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.config.JwtConfig;
import org.catmunity.exception.AuthFailException;
import org.catmunity.exception.LoginFailException;
import org.catmunity.mapper.UserMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.mapper.CatCheckinMapper;
import org.catmunity.mapper.FollowMapper;
import org.catmunity.model.dto.*;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.UserAuthVO;
import org.catmunity.model.vo.UserAdminVO;
import org.catmunity.model.vo.UserLoginVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.UserService;
import org.catmunity.utils.JwtUtil;
import org.catmunity.utils.MailUtils;
import org.catmunity.utils.VerCodeGenerateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.catmunity.constants.CommonConstant.*;
import static org.catmunity.constants.MessageConstant.*;
import static org.catmunity.constants.RedisConstant.*;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CatCheckinMapper catCheckinMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private MailUtils mailUtils;

    @Override
    public PageResult<UserAdminVO> getAdminUserPage(UserQueryDTO queryDTO, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(User::getIdentifier, queryDTO.getKeyword())
                    .or().like(User::getPhone, queryDTO.getKeyword())
                    .or().like(User::getEmail, queryDTO.getKeyword()));
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(User::getType, queryDTO.getType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(User::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getCreatedAtStart() != null) {
            wrapper.ge(User::getCreatedAt, queryDTO.getCreatedAtStart());
        }
        if (queryDTO.getCreatedAtEnd() != null) {
            wrapper.le(User::getCreatedAt, queryDTO.getCreatedAtEnd());
        }

        wrapper.orderByDesc(User::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<UserAdminVO> voList = page.getRecords().stream()
                .map(this::toAdminVO)
                .collect(java.util.stream.Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public UserAdminVO getUserAdminDetail(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new org.catmunity.exception.BusinessException("用户不存在");
        }

        UserAdminVO vo = toAdminVO(user);

        vo.setPostCount(postMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.catmunity.model.entity.Post>()
                .eq(org.catmunity.model.entity.Post::getUserId, userId)));

        vo.setCheckinCount(catCheckinMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.catmunity.model.entity.CatCheckin>()
                .eq(org.catmunity.model.entity.CatCheckin::getUserId, userId)));

        vo.setFollowCount(followMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.catmunity.model.entity.Follow>()
                .eq(org.catmunity.model.entity.Follow::getFollowerId, userId)));

        vo.setFollowerCount(followMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.catmunity.model.entity.Follow>()
                .eq(org.catmunity.model.entity.Follow::getFollowingId, userId)));

        return vo;
    }

    @Override
    @Transactional
    public void banUser(Long userId, String reason) {
        User user = this.getById(userId);
        if (user == null) {
            throw new org.catmunity.exception.BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new org.catmunity.exception.BusinessException("用户已被封禁");
        }

        user.setStatus(1);
        user.setBanReason(reason);
        user.setBannedAt(new Date());
        this.updateById(user);

        redisTemplate.delete(LOGIN_USER_TOKEN + userId);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new org.catmunity.exception.BusinessException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new org.catmunity.exception.BusinessException("用户未被封禁");
        }

        user.setStatus(0);
        user.setBanReason(null);
        user.setBannedAt(null);
        this.updateById(user);
    }

    @Override
    @Transactional
    public void assignUserRole(Long userId, Integer userType) {
        User user = this.getById(userId);
        if (user == null) {
            throw new org.catmunity.exception.BusinessException("用户不存在");
        }
        if (userType < 0 || userType > 2) {
            throw new org.catmunity.exception.BusinessException("无效的用户类型");
        }

        user.setType(userType);
        this.updateById(user);
    }

    @Override
    @Transactional
    public void batchBanUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new org.catmunity.exception.BusinessException("用户ID列表不能为空");
        }

        for (Long userId : userIds) {
            try {
                banUser(userId, "批量封禁");
            } catch (Exception e) {
                log.error("批量封禁用户 {} 失败，错误信息：{}", userId, e.getMessage());
            }
        }
    }

    private UserAdminVO toAdminVO(User user) {
        UserAdminVO vo = new UserAdminVO();
        BeanUtils.copyProperties(user, vo);
        vo.setTypeStr(getTypeStr(user.getType()));
        vo.setStatusStr(user.getStatus() != null && user.getStatus() == 1 ? "已封禁" : "正常");
        return vo;
    }

    private String getTypeStr(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 0: return "超级管理员";
            case 1: return "普通管理员";
            case 2: return "普通用户";
            default: return "未知";
        }
    }
    /**
     * 统一登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {

        // 1. 查询用户（原有逻辑）
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("identifier", userLoginDTO.getIdentifier())
        );
        if (user == null) {
            throw new LoginFailException(USER_NOT_EXIST);
        }

        // 2. 密码校验（原有逻辑）
        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + userLoginDTO.getPassword()).getBytes());
        if (!encryptedPwd.equals(user.getPassword())) {
            throw new LoginFailException(PASSWORD_ERROR);
        }

        // 3. 生成JWT令牌（使用统一方法，与游客令牌格式一致）
        Map<String, Object> claims = new HashMap<>();
        claims.put(LOGIN_USER_ID, user.getId());
        claims.put(LOGIN_USER_TYPE, user.getType());
        Map<String, Object> header = new HashMap<>();
        header.put("type", "jwt");
        String token = jwtUtil.createTokenWithHeader(claims, header); // 带自定义头部

        // 4. 存储到Redis（与游客逻辑一致）
        RedisUserMsgDTO redisUserMsgDTO = new RedisUserMsgDTO();
        BeanUtils.copyProperties(user, redisUserMsgDTO);
        redisUserMsgDTO.setType(user.getType());

        String msgKey = LOGIN_USER_TOKEN + token;
        redisTemplate.opsForValue().set(
                msgKey,
                JSONUtil.toJsonStr(redisUserMsgDTO),
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        String tokenKey = LOGIN_USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(
                tokenKey,
                token,
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        // 5. 返回结果
        return UserLoginVO.builder()
                .token(token)
                .userId(user.getId())
                .type(user.getType())
                .identifier(user.getIdentifier())
                .build();
    }


    /**
     * User info auth: registered users modify their info
     * @param userAuthDTO auth info
     * @return updated user info
     */
    @Override
    @Transactional
    public UserAuthVO authUser(UserAuthDTO userAuthDTO) {
        if (ObjectUtil.isEmpty(userAuthDTO) ||
                StringUtils.isBlank(userAuthDTO.getOldIdentifier()) ||
                StringUtils.isBlank(userAuthDTO.getNewIdentifier()) ||
                StringUtils.isBlank(userAuthDTO.getEmail()) ||
                StringUtils.isBlank(userAuthDTO.getPassword()) ||
                StringUtils.isBlank(userAuthDTO.getCode())) {
            throw new AuthFailException(PARAM_NOT_COMPLETE);
        }

        String email = userAuthDTO.getEmail();
        String inputCode = userAuthDTO.getCode();
        String redisKey = EMAIL_CODE_KEY + email;
        String cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(cachedCode) || !cachedCode.equals(inputCode)) {
            throw new AuthFailException("Code error or expired");
        }

        String oldIdentifier = userAuthDTO.getOldIdentifier();
        String newIdentifier = userAuthDTO.getNewIdentifier();
        Date currentTime = new Date();

        User dbUser = this.lambdaQuery()
                .eq(User::getIdentifier, oldIdentifier)
                .one();

        if (ObjectUtil.isEmpty(dbUser)) {
            throw new AuthFailException(USER_NOT_EXIST);
        }

        if (dbUser.getType() != 0 && dbUser.getType() != 1 && dbUser.getType() != 2) {
            throw new AuthFailException("无效的用户类型");
        }

        User existUser = this.lambdaQuery()
                .eq(User::getIdentifier, newIdentifier)
                .ne(User::getId, dbUser.getId())
                .one();
        if (ObjectUtil.isNotEmpty(existUser)) {
            throw new AuthFailException(NEW_IDENTIFIER_ALREADY_TAKEN);
        }

        User emailUser = this.lambdaQuery()
                .eq(User::getEmail, email)
                .ne(User::getId, dbUser.getId())
                .one();
        if (ObjectUtil.isNotEmpty(emailUser)) {
            throw new AuthFailException(EMAIL_ALREADY_LINKED);
        }

        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + userAuthDTO.getPassword()).getBytes());
        dbUser.setIdentifier(newIdentifier);
        dbUser.setEmail(email);
        dbUser.setPassword(encryptedPwd);
        dbUser.setUpdatedAt(currentTime);
        dbUser.setLastActive(currentTime);
        this.updateById(dbUser);

        return UserAuthVO.builder()
                .userId(dbUser.getId())
                .identifier(dbUser.getIdentifier())
                .type(dbUser.getType())
                .lastActive(dbUser.getLastActive())
                .needLogin(false)
                .build();
    }





    /**
     * 退出登录核心逻辑：清理 Redis 中的登录态缓存
     * @param token 用户登录时的令牌
     */
    @Override
    public void logout(String token) {
        // 1. 定义缓存键（与登录时一致）
        String userInfoKey = LOGIN_USER_TOKEN + token;

        // 2. 获取并解析用户信息，清理 userId 与 token 的映射
        String userInfoJson = redisTemplate.opsForValue().get(userInfoKey);
        if (userInfoJson != null) {
            // 直接解析并删除映射关系
            Long userId = JSONUtil.toBean(userInfoJson, RedisUserMsgDTO.class).getId();
            redisTemplate.delete(LOGIN_USER_TOKEN + userId);
        }

        // 3. 清除 token 对应的用户信息缓存
        redisTemplate.delete(userInfoKey);
    }

    @Override
    @Transactional
    public void updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = this.getById(userId);
        if (user == null) {
            throw new org.catmunity.exception.BusinessException(USER_NOT_EXIST);
        }

        Date currentTime = new Date();

        if (StringUtils.isNotBlank(updateDTO.getNewIdentifier())) {
            User existUser = this.lambdaQuery()
                    .eq(User::getIdentifier, updateDTO.getNewIdentifier())
                    .ne(User::getId, userId)
                    .one();
            if (existUser != null) {
                throw new org.catmunity.exception.BusinessException(NEW_IDENTIFIER_ALREADY_TAKEN);
            }
            user.setIdentifier(updateDTO.getNewIdentifier());
        }

        if (StringUtils.isNotBlank(updateDTO.getNewPassword())) {
            String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + updateDTO.getNewPassword()).getBytes());
            user.setPassword(encryptedPwd);
        }

        user.setUpdatedAt(currentTime);
        user.setLastActive(currentTime);
        this.updateById(user);
    }

    @Override
    public boolean sendRegisterEmailCode(String email) {
        String code = VerCodeGenerateUtil.getVerCode();
        return mailUtils.sendEmailCode(email, code);
    }

    @Override
    @Transactional
    public UserLoginVO register(UserRegisterDTO registerDTO) {
        User existUser = this.lambdaQuery()
                .eq(User::getIdentifier, registerDTO.getIdentifier())
                .one();
        if (existUser != null) {
            throw new org.catmunity.exception.BusinessException(ACCOUNT_ALREADY_EXIST);
        }

        User emailUser = this.lambdaQuery()
                .eq(User::getEmail, registerDTO.getEmail())
                .one();
        if (emailUser != null) {
            throw new org.catmunity.exception.BusinessException(EMAIL_ALREADY_LINKED);
        }

        String redisKey = EMAIL_CODE_KEY + registerDTO.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isBlank(cachedCode) || !cachedCode.equals(registerDTO.getCode())) {
            throw new org.catmunity.exception.BusinessException("验证码错误或已过期");
        }

        Date currentTime = new Date();
        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + registerDTO.getPassword()).getBytes());

        User newUser = User.builder()
                .identifier(registerDTO.getIdentifier())
                .password(encryptedPwd)
                .email(registerDTO.getEmail())
                .type(2)
                .status(0)
                .createdAt(currentTime)
                .updatedAt(currentTime)
                .lastActive(currentTime)
                .build();
        this.save(newUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(LOGIN_USER_ID, newUser.getId());
        claims.put(LOGIN_USER_TYPE, newUser.getType());
        Map<String, Object> header = new HashMap<>();
        header.put("type", "jwt");
        String token = jwtUtil.createTokenWithHeader(claims, header);

        RedisUserMsgDTO redisUserMsgDTO = new RedisUserMsgDTO();
        BeanUtils.copyProperties(newUser, redisUserMsgDTO);
        redisUserMsgDTO.setType(newUser.getType());

        String msgKey = LOGIN_USER_TOKEN + token;
        redisTemplate.opsForValue().set(
                msgKey,
                JSONUtil.toJsonStr(redisUserMsgDTO),
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        String tokenKey = LOGIN_USER_TOKEN + newUser.getId();
        redisTemplate.opsForValue().set(
                tokenKey,
                token,
                jwtConfig.getUserTtlMillis() / 1000,
                TimeUnit.SECONDS
        );

        return UserLoginVO.builder()
                .token(token)
                .userId(newUser.getId())
                .type(newUser.getType())
                .identifier(newUser.getIdentifier())
                .build();
    }


}