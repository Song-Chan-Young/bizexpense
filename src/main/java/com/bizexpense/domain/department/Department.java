package com.bizexpense.domain.department;

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

@Getter
@Entity
@Table(name = "department")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(name = "department_name", nullable = false, length = 100)
    private String name;

    @Column(name = "department_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "use_yn", nullable = false)
    private boolean active = true;

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
