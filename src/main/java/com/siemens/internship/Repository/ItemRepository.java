package com.siemens.internship.Repository;

import com.siemens.internship.Domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT id FROM Item")
    List<Long> findAllIds();
}
