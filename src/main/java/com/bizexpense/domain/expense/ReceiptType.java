package com.bizexpense.domain.expense;

import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 첨부할 수 있는 영수증 형식. 브라우저가 보낸 Content-Type 은 믿지 않고,
 * 확장자와 파일 앞부분(매직 넘버)이 모두 맞아야 받는다.
 */
@Getter
@RequiredArgsConstructor
public enum ReceiptType {

    JPEG("image/jpeg", List.of("jpg", "jpeg")),
    PNG("image/png", List.of("png")),
    GIF("image/gif", List.of("gif")),
    WEBP("image/webp", List.of("webp")),
    PDF("application/pdf", List.of("pdf"));

    private final String contentType;
    private final List<String> extensions;

    public static ReceiptType detect(String fileName, byte[] data) {
        String ext = extension(fileName);
        ReceiptType type = Arrays.stream(values())
                .filter(t -> t.extensions.contains(ext))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_FILE_TYPE));
        if (!type.matches(data)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "파일 내용이 확장자(" + ext + ")와 맞지 않습니다.");
        }
        return type;
    }

    private boolean matches(byte[] d) {
        return switch (this) {
            case JPEG -> startsWith(d, 0, 0xFF, 0xD8, 0xFF);
            case PNG -> startsWith(d, 0, 0x89, 'P', 'N', 'G');
            case GIF -> startsWith(d, 0, 'G', 'I', 'F', '8');
            case WEBP -> startsWith(d, 0, 'R', 'I', 'F', 'F') && startsWith(d, 8, 'W', 'E', 'B', 'P');
            case PDF -> startsWith(d, 0, '%', 'P', 'D', 'F');
        };
    }

    private static boolean startsWith(byte[] data, int offset, int... signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
