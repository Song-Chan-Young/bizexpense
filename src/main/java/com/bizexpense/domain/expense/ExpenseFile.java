package com.bizexpense.domain.expense;

import com.bizexpense.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경비 영수증 파일. 체험 서버는 재시작하면 디스크가 비워지므로 파일 내용도 DB 에 저장한다.
 * 목록 조회는 내용(data)을 빼고 메타 정보만 읽는다 ({@link ExpenseFileRepository#findInfos}).
 */
@Getter
@Entity
@Table(name = "expense_file", indexes = @Index(name = "idx_expense_file_expense", columnList = "expense_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseFile extends BaseTimeEntity {

    /** 파일 1개 최대 크기 (5MB) */
    public static final int MAX_SIZE = 5 * 1024 * 1024;
    /** 경비 1건에 첨부할 수 있는 최대 파일 수 */
    public static final int MAX_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReceiptType type;

    @Column(nullable = false)
    private long size;

    /** PostgreSQL 과 H2(PostgreSQL 모드) 모두 bytea 를 쓴다. 기본 매핑(blob)은 H2 PostgreSQL 모드에서 지원하지 않는다. */
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public ExpenseFile(Expense expense, String originalName, ReceiptType type, byte[] data) {
        this.expense = expense;
        this.originalName = originalName;
        this.type = type;
        this.size = data.length;
        this.data = data;
    }
}
