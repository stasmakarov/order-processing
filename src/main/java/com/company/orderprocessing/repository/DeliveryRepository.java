package com.company.orderprocessing.repository;

import com.company.orderprocessing.entity.Delivery;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryRepository extends JmixDataRepository<Delivery, UUID> {
    @Query("select d from ord_Delivery d where d.number = :number")
    Delivery findByNumber(@Param("number") String number);
}