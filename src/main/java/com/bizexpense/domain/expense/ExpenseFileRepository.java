package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.ExpenseFileResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseFileRepository extends JpaRepository<ExpenseFile, Long> {

    /** 파일 내용 없이 메타 정보만 조회 */
    @Query("""
            select new com.bizexpense.domain.expense.dto.ExpenseFileResponse(
                f.id, f.originalName, f.type, f.size, f.createdAt)
            from ExpenseFile f
            where f.expense.id = :expenseId
            order by f.id
            """)
    List<ExpenseFileResponse> findInfos(@Param("expenseId") Long expenseId);

    long countByExpenseId(Long expenseId);

    Optional<ExpenseFile> findByIdAndExpenseId(Long id, Long expenseId);

    /** 일괄 삭제 후 영속성 컨텍스트를 비워, 이미 읽어 둔 파일 엔티티가 남지 않게 한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExpenseFile f where f.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);
}
