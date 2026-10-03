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

/** 비용 항목 (교통비, 식비, 숙박비 …). 사용 중지해도 기존 경비 데이터는 그대로 유지된다. */
@Getter
@Entity
@Table(name = "expense_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "use_yn", nullable = false)
    private boolean active = true;

    public ExpenseCategory(String name) {
        this.name = name;
    }

    public void update(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}
