package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.ExpenseFileResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.security.LoginUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses/{expenseId}/files")
public class ExpenseFileController {

    private final ExpenseFileService fileService;

    @GetMapping
    public ApiResponse<List<ExpenseFileResponse>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                       @PathVariable Long expenseId) {
        return ApiResponse.ok(fileService.list(loginUser, expenseId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseFileResponse> upload(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long expenseId,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(loginUser, expenseId, file));
    }

    /** 파일 내용. 이미지·PDF 는 브라우저에서 바로 보이도록 inline 으로 보낸다. */
    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long expenseId, @PathVariable Long fileId) {
        ExpenseFile file = fileService.download(loginUser, expenseId, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getType().getContentType()))
                .contentLength(file.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(file.getData());
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long expenseId, @PathVariable Long fileId) {
        fileService.delete(loginUser, expenseId, fileId);
        return ApiResponse.ok();
    }
}
