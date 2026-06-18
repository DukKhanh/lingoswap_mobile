package com.lingoswap.utils;

/**
 * ImageUtils — chuẩn hoá URL ảnh trước khi nạp bằng Glide.
 *
 * Backend sinh avatar mặc định bằng dicebear ở định dạng SVG
 * (vd: https://api.dicebear.com/7.x/adventurer/svg?seed=...). Glide KHÔNG decode
 * được SVG → đổi sang PNG để hiển thị được. Ảnh upload (Cloudinary PNG/JPG) không
 * chứa "svg" nên không bị ảnh hưởng.
 */
public final class ImageUtils {

    private ImageUtils() {}

    public static String normalizeAvatar(String url) {
        if (url == null || url.isEmpty()) return url;
        String out = url;
        if (out.contains("/svg?")) {
            out = out.replace("/svg?", "/png?");
        } else if (out.endsWith("/svg")) {
            out = out.substring(0, out.length() - 4) + "/png";
        }
        out = out.replace("format=svg", "format=png");
        return out;
    }
}
