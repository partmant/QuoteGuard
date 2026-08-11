package com.project.back.domain.demo;

import com.project.back.domain.user.entity.User;
import com.project.back.domain.user.entity.UserRole;
import com.project.back.domain.user.entity.UserStatus;
import com.project.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 포트폴리오 공개 데모용 고정 계정을 서버 기동 시점에 준비한다.
 *
 * <p>일반 회원가입 흐름은 이메일 링크로 초기 비밀번호를 설정해야 로그인이 가능하지만
 * (관리자가 계정 생성 → passwordInitialized=false → 이메일 인증 필요), 데모 계정은
 * 방문자가 이메일 인증 없이 바로 로그인해 볼 수 있도록 passwordInitialized=true 상태로 생성한다.
 *
 * <p>demo.enabled=true (기본값 false)일 때만 동작하며, demo.*.password가 비어 있으면
 * 안전을 위해 해당 계정 생성을 건너뛴다. 이미 존재하는 이메일이면 다시 만들지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true")
public class DemoAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.department:demo_department}")
    private String demoDepartment;

    @Value("${demo.staff.email:demo.staff@quoteguard.com}")
    private String staffEmail;

    @Value("${demo.staff.password:}")
    private String staffPassword;

    @Value("${demo.manager.email:demo.manager@quoteguard.com}")
    private String managerEmail;

    @Value("${demo.manager.password:}")
    private String managerPassword;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 승인 흐름(요청자↔승인권자)을 보여주려면 두 역할이 같은 부서여야 하므로 함께 생성한다.
        ensureDemoAccount(staffEmail, staffPassword, "데모 영업사원", UserRole.SALES_STAFF);
        ensureDemoAccount(managerEmail, managerPassword, "데모 승인권자", UserRole.SALES_MANAGER);
    }

    private void ensureDemoAccount(String email, String rawPassword, String name, UserRole role) {
        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("데모 계정 비밀번호({} 관련 demo.*.password)가 설정되지 않아 계정 생성을 건너뜁니다.", email);
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User demoUser = User.builder()
                .memberNumber(generateDemoMemberNumber())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .department(demoDepartment)
                .position("demo_position")
                .role(role)
                .status(UserStatus.ACTIVE)
                .passwordInitialized(true) // 이메일 인증 없이 즉시 로그인 가능하게
                .build();

        userRepository.save(demoUser);
        log.info("데모 계정 생성 완료: {} ({})", email, role);
    }

    // 실제 회원번호(YY+5자리 난수)와 겹치지 않도록 DEMO 접두사를 사용한다.
    private String generateDemoMemberNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "DEMO" + String.format("%04d", SECURE_RANDOM.nextInt(10_000));
            if (!userRepository.existsByMemberNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("데모 계정 회원번호 생성에 반복적으로 실패했습니다.");
    }
}
