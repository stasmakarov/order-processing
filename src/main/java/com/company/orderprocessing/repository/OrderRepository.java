package com.company.orderprocessing.repository;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.entity.OrderStatus;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JmixDataRepository<Order, Integer> {
    @Override
    void deleteById(Integer integer);

    @Query("SELECT o FROM ord_Order o WHERE o.delivery = :delivery")
    List<Order> findByDelivery(@Param("delivery") Delivery delivery);

    @Query("SELECT COUNT(o) FROM ord_Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Integer status);

    @Query("SELECT COUNT(o) FROM ord_Order o")
    long countTotal();
}