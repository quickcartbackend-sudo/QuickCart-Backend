package com.quickcart.user.repository;

import com.quickcart.user.entity.Address;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByDefaultAddressDescIdAsc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findFirstByUserIdOrderByIdAsc(Long userId);

    long countByUserId(Long userId);

    /** Bulk update bypasses auditing, so updatedAt is set explicitly. */
    @Modifying(flushAutomatically = true)
    @Query("update Address a set a.defaultAddress = false, a.updatedAt = :now "
            + "where a.userId = :userId and a.defaultAddress = true")
    void clearDefaults(@Param("userId") Long userId, @Param("now") Instant now);
}