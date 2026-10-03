package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.ExpenseFileResponse;
import com.bizexpense.domain.user.AccessPolicy;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 경비 영수증 첨부. 조회는 경비를 볼 수 있는 사람(본인·같은 부서 팀장·관리자),
 * 첨부/삭제는 본인이 수정할 수 있는 상태(임시저장·반려)의 경비에만 할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseFileService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseFileRepository fileRepository;

    public List<ExpenseFileResponse> list(LoginUser loginUser, Long expenseId) {
        Expense expense = findExpense(expenseId);
        AccessPolicy.checkReadable(loginUser, expense.getUser());
        return fileRepository.findInfos(expenseId);
    }

    public ExpenseFile download(LoginUser loginUser, Long expenseId, Long fileId) {
        Expense expense = findExpense(expenseId);
        AccessPolicy.checkReadable(loginUser, expense.getUser());
        return findFile(expenseId, fileId);
    }

    @Transactional
    public ExpenseFileResponse upload(LoginUser loginUser, Long expenseId, MultipartFile file) {
        Expense expense = findExpense(expenseId);
        AccessPolicy.checkOwner(loginUser, expense.getUser());
        expense.checkEditable("영수증을 첨부");
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "첨부할 파일을 선택하세요.");
        }
        if (file.getSize() > ExpenseFile.MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (fileRepository.countByExpenseId(expenseId) >= ExpenseFile.MAX_COUNT) {
            throw new BusinessException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        String name = originalName(file);
        byte[] data = bytes(file);
        ExpenseFile saved = fileRepository.save(new ExpenseFile(expense, name, ReceiptType.detect(name, data), data));
        expense.markProof(true);
        return new ExpenseFileResponse(saved.getId(), saved.getOriginalName(), saved.getType(),
                saved.getSize(), saved.getCreatedAt());
    }

    @Transactional
    public void delete(LoginUser loginUser, Long expenseId, Long fileId) {
        Expense expense = findExpense(expenseId);
        AccessPolicy.checkOwner(loginUser, expense.getUser());
        expense.checkEditable("영수증을 삭제");
        fileRepository.delete(findFile(expenseId, fileId));
        fileRepository.flush();
        expense.markProof(fileRepository.countByExpenseId(expenseId) > 0);
    }

    private Expense findExpense(Long expenseId) {
        return expenseRepository.findDetailById(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
    }

    private ExpenseFile findFile(Long expenseId, Long fileId) {
        return fileRepository.findByIdAndExpenseId(fileId, expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    /** 경로가 붙어 오는 브라우저(구형 IE 등)를 고려해 파일 이름만 남긴다. */
    private static String originalName(MultipartFile file) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/')));
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "파일 이름이 없습니다.");
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
