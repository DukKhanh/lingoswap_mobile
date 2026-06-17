package com.lingoswap.utils;

import org.json.JSONObject;
import retrofit2.Response;

public class ErrorUtils {
    public static String parseError(Response<?> response) {
        int code = response.code();
        
        // ISSUE 6 FIX: Phân loại lỗi theo HTTP Status Code
        switch (code) {
            case 401:
                return "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
            case 403:
                return "Tài khoản của bạn đã bị khóa hoặc không có quyền truy cập.";
            case 404:
                return "Dữ liệu không tồn tại trên hệ thống.";
            case 500:
                return "Lỗi hệ thống phía máy chủ. Vui lòng thử lại sau.";
            default:
                try {
                    String errorBody = response.errorBody().string();
                    JSONObject json = new JSONObject(errorBody);
                    return json.optString("error", "Đã xảy ra lỗi (" + code + ")");
                } catch (Exception e) {
                    return "Lỗi không xác định (" + code + ")";
                }
        }
    }
}
