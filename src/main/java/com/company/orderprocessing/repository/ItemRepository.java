package com.company.orderprocessing.repository;

import com.company.orderprocessing.entity.Item;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JmixDataRepository<Item, UUID> {

    @Query("select e from ord_Item e where e.name = :name")
    Optional<Item> findByName(@Param("name") String name);

}