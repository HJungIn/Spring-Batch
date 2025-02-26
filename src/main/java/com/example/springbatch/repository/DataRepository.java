package com.example.springbatch.repository;

import com.example.springbatch.model.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataRepository extends JpaRepository<Data, Long> {
    @Modifying(clearAutomatically = true) // clearAutomatically = true 설정하면 영속성 컨텍스트가 자동으로 비워짐
    @Query("update Data set title = :title where id=:id")
    void updateTitle(@Param(value = "id") Long id, @Param(value = "title") String title);
}
