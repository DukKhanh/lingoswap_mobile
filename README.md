# LingoSwap – Android Studio Project

## Cấu trúc dự án

```
LingoSwap/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lingoswap/activities/
│       │   ├── SignInActivity.java          ← Screen 1
│       │   ├── SignUpActivity.java          ← Screen 2
│       │   ├── ForgotPasswordActivity.java  ← Screen 3
│       │   ├── ResetPasswordActivity.java   ← Screen 4
│       │   ├── ResetSuccessActivity.java    ← Screen 5
│       │   ├── HomeActivity.java            ← Screen 6
│       │   ├── LanguageChooserDialog.java   ← Screen 7 (BottomSheet)
│       │   ├── MatchingActivity.java        ← Screen 8
│       │   ├── VideoCallActivity.java       ← Screen 9
│       │   ├── RatingActivity.java          ← Screen 10
│       │   └── ProfileActivity.java         ← Screen 11
│       └── res/
│           ├── drawable/      ← 28+ shape/color drawables
│           ├── layout/        ← 11 activity XML files + 1 dialog
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── styles.xml
├── build.gradle
└── settings.gradle
```

## Hướng dẫn import vào Android Studio

1. Mở Android Studio → **File → Open** → chọn thư mục `LingoSwap`
2. Đợi Gradle sync xong (lần đầu có thể mất vài phút)
3. Kết nối thiết bị Android (API 24+) hoặc tạo AVD emulator
4. Nhấn **Run ▶** để chạy app

## Dependencies đã dùng

```groovy
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

## Màu sắc chính (colors.xml)

| Tên          | Hex       | Dùng cho              |
|--------------|-----------|-----------------------|
| blue         | #3B82F6   | Primary, buttons, links |
| blue_light   | #EFF6FF   | Selected backgrounds  |
| text_dark    | #111827   | Headings              |
| text_muted   | #6B7280   | Subtitles, hints      |
| input_bg     | #F1F5F9   | Input backgrounds     |
| green        | #10B981   | Online status         |
| red          | #EF4444   | Busy, End call        |
| yellow       | #F59E0B   | Star ratings          |

## TODO – Tích hợp backend

- **SignInActivity**: Thay `// TODO: call your auth API` bằng Retrofit/OkHttp call
- **SignUpActivity**: Gọi API đăng ký
- **ForgotPasswordActivity**: Gọi API gửi OTP
- **ResetPasswordActivity**: Gọi API verify OTP + reset password
- **VideoCallActivity**: Tích hợp WebRTC (thư viện `io.getstream:stream-webrtc-android`)
- **HomeActivity – Friends list**: Dùng RecyclerView + Adapter để load dữ liệu thật
- **MatchingActivity**: Kết nối socket (Socket.IO hoặc WebSocket) để tìm partner

## Quyền (AndroidManifest)

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

Nhớ xin quyền CAMERA và RECORD_AUDIO lúc runtime trên Android 6+.
