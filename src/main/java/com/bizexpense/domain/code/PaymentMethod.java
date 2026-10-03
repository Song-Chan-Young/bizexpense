package com.bizexpense.domain.code;

import com.bizexpense.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 수단 (법인카드, 개인카드, 현금, 계좌이체).
 * corporate=true 인 수단은 회사가 이미 결제한 것이므로 정산 시 개인 지급액에서 제외된다.
 */
@Getter
@Entity
@Table(name = "payment_method")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_method_id")
    private Long id;

    @Column(name = "payment_method_name", nullable = false, unique = true, length = 50)
    private String name;

    /** 법인 결제 여부 (정산 계산 기준) */
    @Column(name = "corporate_yn", nullable = false)
    private boolean corporate;

    @Column(name = "use_yn", nullable = false)
    private boolean active = true;

    public PaymentMethod(String name, boolean corporate) {
        this.name = name;
        this.corporate = corporate;
    }

    public void update(String name, boolean corporate, boolean active) {
        this.name = name;
        this.corporate = corporate;
        this.active = active;
    }
}
