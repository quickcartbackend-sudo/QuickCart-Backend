package com.quickcart.user.mapper;

import com.quickcart.user.dto.response.UserProfile;
import com.quickcart.user.dto.response.UserSummary;
import com.quickcart.user.entity.User;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }

    public UserProfile toProfile(User u) {
        return new UserProfile(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}