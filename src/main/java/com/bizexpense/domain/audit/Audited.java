package com.bizexpense.domain.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드에 붙이면 요청 결과(성공/실패)를 감사 로그로 남긴다. ({@link AuditAspect})
 * 변경 요청(등록·수정·삭제·결재·로그인)에만 붙인다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    AuditAction value();
}
