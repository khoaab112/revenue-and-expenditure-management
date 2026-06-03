# Quản Lý Chi Tiêu (Finance Manager)

Đây là mã nguồn dự án ứng dụng Android quản lý tài chính cá nhân, hỗ trợ theo dõi thu chi, quản lý ví, hạn mức chi tiêu (budget), dòng thời gian (timeline), và nhiều tính năng khác. Ứng dụng được xây dựng hoàn toàn bằng **Kotlin** và giao diện **Jetpack Compose**.

## Yêu cầu công cụ
Để mở và chạy được mã nguồn này trên máy tính của bạn, bạn cần:
- **Android Studio**: Tải xuống và cài đặt phiên bản mới nhất (Google khuyến nghị bản Ladybug hoặc Koala).
- **Java Development Kit (JDK 17)**: Thường được tích hợp sẵn khi cài Android Studio.

## Hướng dẫn cài đặt và chạy dự án (Run chạy thử nghiệm)

1. **Tải mã nguồn về máy**: Tải thư mục chứa tất cả các tệp tin của dự án (.zip) từ AI Studio hoặc từ Github và giải nén.
2. **Mở dự án trong Android Studio**:
   - Mở phần mềm Android Studio.
   - Ở màn hình Welcome, chọn **Open** (hoặc vào menu `File > Open...`).
   - Điều hướng tới thư mục mã nguồn vừa giải nén và nhấn **OK**.
3. **Đồng bộ hóa Gradle (Sync):** 
   - Khi vừa mở, Android Studio sẽ tự động tải thư viện và cấu hình dự án (Gradle Sync). Hãy đảm bảo máy tính bạn có kết nối internet và chờ đến khi thanh tiến trình chạy xong.
4. **Thiết bị chạy (Device):**
   - **Máy ảo (Emulator):** Vào `Tools -> Device Manager` -> Bấm `Create Device` để tạo 1 chiếc điện thoại ảo Android.
   - **Máy thật:** Cắm cáp điện thoại Android vào máy tính. Trên điện thoại, vào Cài đặt -> Tùy chọn nhà phát triển -> Bật **Gỡ lỗi USB** (USB Debugging).
5. **Chạy ứng dụng:**
   - Click vào nút **Run** (hình tam giác màu xanh lá cây) trên thanh công cụ phía trên cùng, hoặc nhấn `Shift + F10`.
   - Ứng dụng sẽ được biên dịch và cài đặt tự động lên máy thật hoặc máy ảo.

---

## Hướng dẫn xuất file cài đặt (APK)

Nếu bạn muốn tạo file `.apk` để gửi cho bạn bè cài đặt hoặc lưu trữ mà không cần đưa lên CH Play:

### Build APK gỡ lỗi (Debug APK) - Nhanh gọn
Đây là cách tạo APK nhanh nhất để test trên điện thoại khác:
1. Mở Android Studio.
2. Trên menu trên cùng, chọn **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
3. Chờ công cụ Gradle xử lý ở dưới cùng màn hình. Khi hoàn tất (khoảng 1 - vài phút), sẽ có một pop-up thông báo hiển thị ở góc dưới bên phải.
4. Bấm vào chữ **locate** trên pop-up thông báo. Hệ thống sẽ mở thư mục chứa file APK (thường nằm theo đường dẫn: `app/build/outputs/apk/debug/app-debug.apk`).
5. Bạn có thể gửi file `app-debug.apk` vào điện thoại qua Zalo, Drive, hoặc copy qua cáp để cài đặt (nhớ cho phép điện thoại cài đặt từ "Nguồn không xác định").

### Build APK phát hành (Release APK / Signed APK) - Đủ chuẩn đẩy lên CH Play
Nếu bạn muốn đóng gói app chính thức:
1. Chọn menu **Build** -> **Generate Signed Bundle / APK...**
2. Chọn **APK** (nếu muốn lấy file cài) hoặc **Android App Bundle** (nếu muốn đẩy lên Google Play) -> Bấm **Next**.
3. Dưới mục *Key store path*, bấm **Create new...** để tạo một tệp chìa khóa (keystore) bảo mật cho app. Điền mật khẩu và thông tin cá nhân.
4. Bấm **Next**, tích chọn bản **release** -> Chọn **Create**.
5. Đợi hệ thống build xong, chọn **locate** để mở thư mục chứa file `app-release.apk`. Đây là phiên bản nén tốt nhất, chạy mượt nhất, đã được ký bảo mật của bạn.
