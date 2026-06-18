package com.lingoswap.utils;

import org.json.JSONObject;
import retrofit2.Response;

public class ErrorUtils {

    /**
     * Ưu tiên hiển thị ĐÚNG message lỗi mà backend trả về (field "error" hoặc
     * "message") cho MỌI mã lỗi — ví dụ login 401 phải hiện "Email hoặc mật khẩu
     * không chính xác", change-password 400 "Mật khẩu hiện tại không chính xác".
     * Chỉ khi không đọc được body mới fallback sang thông báo chung theo HTTP code.
     */
    public static String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    JSONObject json = new JSONObject(raw);
                    String msg = json.optString("error", null);
                    if (msg == null || msg.isEmpty()) msg = json.optString("message", null);
                    if (msg != null && !msg.isEmpty()) return msg;
                }
            }
        } catch (Exception ignored) {
            // body không phải JSON → rơi xuống fallback theo code
        }

        int code = response.code();
        switch (code) {
            case 401: return "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
            case 403: return "Bạn không có quyền thực hiện hành động này.";
            case 404: return "Dữ liệu không tồn tại trên hệ thống.";
            case 500: return "Lỗi hệ thống phía máy chủ. Vui lòng thử lại sau.";
            default:  return "Đã xảy ra lỗi (" + code + ")";
        }
    }
}
