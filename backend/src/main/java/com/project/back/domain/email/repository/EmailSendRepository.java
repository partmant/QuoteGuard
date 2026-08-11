package com.project.back.domain.email.repository;

import com.project.back.domain.email.entity.EmailSend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailSendRepository extends JpaRepository<EmailSend, Long> {

    @Query("SELECT e FROM EmailSend e JOIN FETCH e.quote " +
            "WHERE e.sentBy.id = :userId ORDER BY e.createdAt DESC")
    List<EmailSend> findBySentByIdWithQuote(@Param("userId") Long userId);

    // 데모 데이터 초기화 시 견적 삭제 전에 관련 발송 이력을 먼저 정리하기 위한 메서드
    void deleteByQuoteId(Long quoteId);
}
