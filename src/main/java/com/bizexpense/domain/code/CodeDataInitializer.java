package com.bizexpense.domain.code;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기준 코드 초기 데이터. 모든 환경(운영 포함)에서 필요하므로 프로필과 관계없이 실행하고,
 * 테이블이 비어 있을 때만 넣는다. 이후 변경은 관리자 화면에서 한다.
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class CodeDataInitializer implements ApplicationRunner {

    private static final List<String> CATEGORIES = List.of("교통비", "식비", "숙박비", "접대비", "소모품비", "통신비", "기타");

    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            CATEGORIES.forEach(name -> categoryRepository.save(new ExpenseCategory(name)));
            log.info("비용 항목 초기 데이터 생성: {}", CATEGORIES);
        }
        if (paymentMethodRepository.count() == 0) {
            paymentMethodRepository.save(new PaymentMethod("법인카드", true));
            paymentMethodRepository.save(new PaymentMethod("개인카드", false));
            paymentMethodRepository.save(new PaymentMethod("현금", false));
            paymentMethodRepository.save(new PaymentMethod("계좌이체", false));
            log.info("결제 수단 초기 데이터 생성");
        }
    }
}
