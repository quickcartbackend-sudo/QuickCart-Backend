package com.quickcart.order.repository;

import com.quickcart.order.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartUserIdOrderByIdAsc(Long userId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    boolean existsByCartUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from CartItem i where i.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}