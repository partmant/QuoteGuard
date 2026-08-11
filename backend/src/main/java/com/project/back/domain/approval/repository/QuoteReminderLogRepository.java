package com.project.back.domain.approval.repository;

import com.project.back.domain.approval.entity.QuoteReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteReminderLogRepository extends JpaRepository<QuoteReminderLog, Long> {

    boolean existsByQuoteIdAndTriggerType(Long quoteId, QuoteReminderLog.TriggerType triggerType);

    // 데모 데이터 초기화 시 견적 삭제 전에 관련 리마인더 이력을 먼저 정리하기 위한 메서드
    void deleteByQuoteId(Long quoteId);
}
