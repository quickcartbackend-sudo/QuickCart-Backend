package com.quickcart.order.repository;

import com.quickcart.order.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select new com.quickcart.order.repository.OrderItemCount(i.order.id, sum(i.quantity)) "
            + "from OrderItem i where i.order.id in :ids group by i.order.id")
    List<OrderItemCount> countByOrderIds(@Param("ids") Collection<Long> ids);
}