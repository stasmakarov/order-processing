package com.company.orderprocessing.repository;

import com.company.orderprocessing.entity.Numerator;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NumeratorRepository extends JmixDataRepository<Numerator, UUID> {

    // Method to find the first (or only) Numerator entity
    @Query("SELECT n FROM Numerator n")
    List<Numerator> findAllNumerators();

    // Method to get the next value of the numerator
    default Integer getNextValue() {
        List<Numerator> numerators = findAllNumerators();
        if (!numerators.isEmpty()) {
            // Assuming there is only one numerator, get its value and increment it
            return numerators.get(0).getNumber() + 1;
        }
        // If no numerators exist, return 1 as the starting point
        return 1;
    }
}

