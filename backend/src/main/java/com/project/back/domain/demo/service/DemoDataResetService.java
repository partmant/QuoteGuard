package com.project.back.domain.demo.service;

import com.project.back.domain.approval.repository.ApprovalRequestRepository;
import com.project.back.domain.approval.repository.QuoteReminderLogRepository;
import com.project.back.domain.customer.repository.CustomerRepository;
import com.project.back.domain.email.repository.EmailSendRepository;
import com.project.back.domain.quote.entity.Quote;
import com.project.back.domain.quote.repository.QuoteRepository;
import com.project.back.domain.user.entity.User;
import com.project.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공개 데모 계정(demo.staff / demo.manager)이 만든 데이터를 초기화한다.
 *
 * <p>데모 계정은 삭제·생성을 포함한 모든 기능을 실제 계정과 동일하게 제한 없이 쓸 수 있게 두는 대신,
 * 방문자가 남긴 데이터를 주기적으로 비워서 다음 방문자에게 항상 깨끗한 상태를 보여주는 방식을 선택.
 *
 * <p>삭제 순서는 FK 제약을 따른다: 견적(quote) 하위의 승인 요청 → 승인 이력(JPA cascade)·
 * 이메일 발송 이력·리마인더 발송 이력을 먼저 지운 뒤 견적을 지우고(품목·승인사유는 cascade+orphanRemoval),
 * 마지막으로 고객을 지운다(고객은 견적에서 참조 중이면 삭제할 수 없으므로 반드시 견적을 먼저 지워야 한다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataResetService {

    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final CustomerRepository customerRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final EmailSendRepository emailSendRepository;
    private final QuoteReminderLogRepository quoteReminderLogRepository;

    @Value("${demo.staff.email:demo.staff@quoteguard.com}")
    private String staffEmail;

    @Value("${demo.manager.email:demo.manager@quoteguard.com}")
    private String managerEmail;

    @Transactional
    public void resetDemoData() {
        List<User> demoUsers = userRepository.findAllByEmailIn(List.of(staffEmail, managerEmail));
        if (demoUsers.isEmpty()) {
            log.info("초기화할 데모 계정이 없습니다.");
            return;
        }

        // 1단계: 모든 데모 계정의 견적(및 하위 데이터)을 먼저 정리한다.
        for (User demoUser : demoUsers) {
            deleteQuotesOf(demoUser);
        }

        // 2단계: 견적이 모두 정리된 뒤에 고객을 정리한다 (customer_id FK ON DELETE RESTRICT).
        int deletedCustomers = 0;
        for (User demoUser : demoUsers) {
            var customers = customerRepository.findByCreatedById(demoUser.getId());
            customerRepository.deleteAll(customers);
            deletedCustomers += customers.size();
        }

        log.info("데모 데이터 초기화 완료 [대상 계정={}명, 삭제된 고객={}건]", demoUsers.size(), deletedCustomers);
    }

    private void deleteQuotesOf(User demoUser) {
        List<Quote> quotes = quoteRepository.findByCreatedByOrderByCreatedAtDesc(demoUser);
        for (Quote quote : quotes) {
            // 승인 요청을 엔티티 단위로 삭제해 JPA cascade로 승인 이력(QuoteApprovalHistory)까지 함께 제거
            approvalRequestRepository.findByQuote_Id(quote.getId())
                    .ifPresent(approvalRequestRepository::delete);
            emailSendRepository.deleteByQuoteId(quote.getId());
            quoteReminderLogRepository.deleteByQuoteId(quote.getId());
        }
        // 견적 품목(QuoteItem)·승인사유(QuoteApprovalReason)는 cascade+orphanRemoval로 함께 삭제된다.
        quoteRepository.deleteAll(quotes);

        if (!quotes.isEmpty()) {
            log.info("데모 계정 [{}] 견적 {}건 삭제", demoUser.getEmail(), quotes.size());
        }
    }
}
