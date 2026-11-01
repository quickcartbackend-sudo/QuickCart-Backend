package com.quickcart.user.service;

import com.quickcart.user.dto.request.UpdateProfileRequest;
import com.quickcart.user.dto.response.UserProfile;
import com.quickcart.user.entity.User;
import com.quickcart.user.exception.ApiException;
import com.quickcart.user.mapper.UserMapper;
import com.quickcart.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(long userId) {
        return userMapper.toProfile(loadActive(userId));
    }

    @Transactional
    public UserProfile updateProfile(long userId, UpdateProfileRequest req) {
        User user = loadActive(userId);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        return userMapper.toProfile(user);     // dirty checking flushes on commit
    }

    // a valid token for a deleted or deactivated user is treated as unauthenticated
    private User loadActive(long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication is required."));
    }
}