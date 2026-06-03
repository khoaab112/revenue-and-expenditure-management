# Quản Lý Chi Tiêu (Finance Manager)

Đây là mã nguồn dự án ứng dụng Android quản lý tài chính cá nhân toàn diện, giúp bạn dễ dàng theo dõi dòng tiền, kiểm soát ngân sách và tối ưu hóa thói quen chi tiêu. Ứng dụng được thiết kế với giao diện hiện đại, trực quan mang lại trải nghiệm mượt mà, được xây dựng hoàn toàn bằng ngôn ngữ **Kotlin** và bộ công cụ giao diện UI hiện đại **Jetpack Compose**.

## 🌟 Chức năng chính (Features)
- **Bảng điều khiển (Dashboard):** Xem nhanh tổng quan số dư tài khoản, báo cáo thu chi trong tháng, các hạn mức ngân sách và mục tiêu tiết kiệm nổi bật.
- **Quản lý Giao dịch:** Thêm, xem, sửa, xoá giao dịch thu/chi nhanh chóng với danh mục và biểu tượng (icon) phong phú. Hỗ trợ tạo giao dịch chuyển tiền nhanh giữa các ví.
- **Dòng thời gian (Timeline) & Lịch sử:** Xem lịch sử giao dịch trực quan theo từng ngày với biểu đồ dạng dòng thời gian đẹp mắt ghép nối sát sao với tổng thu/chi theo ngày.
- **Quản lý Ví (Wallets):** Ghi chép nhiều tài khoản/ví riêng biệt.
- **Hạn mức chi tiêu (Budgets):** Đặt hạn mức chi tiêu định kỳ theo tháng cho nhiều danh mục. Theo dõi lượng tiền còn lại với biểu đồ thanh sọc (striped progress) trực quan và màu sắc cảnh báo khi sắp vượt quá tiêu chuẩn.
- **Mục tiêu tiết kiệm (Savings):** Khởi tạo và đồng hành những mục tiêu tài chính, theo dõi tiến độ hoàn thành các quỹ tiết kiệm của bản thân.
- **Quét thông báo (Scan Notifications):** Hỗ trợ công cụ đọc và bóc tách biến động số dư từ những SMS hoặc thông báo đẩy của ngân hàng để nhập liệu tự động vào sổ cái.
- **Báo cáo (Reports):** Hệ thống tạo báo cáo thu/chi trực quan qua biểu đồ tròn, biểu đồ cột; cho phép người dùng tùy chọn theo khoảng thời gian, nhóm ví, và các hạng mục phân tích.
- **Bảo mật bằng mã PIN:** Khóa nhanh ứng dụng để bảo vệ quyền riêng tư số liệu trước truy cập trái phép bằng mật khẩu PIN 4 chữ số.

---

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

---

## Hướng dẫn Build bằng Command Line (Terminal) trên Ubuntu / Linux

Hoàn toàn được! Bạn không bắt buộc phải dùng giao diện (GUI) của Android Studio. Trên Ubuntu, bạn có thể build dự án hoàn toàn thông qua công cụ dòng lệnh **Gradle Wrapper** được tích hợp sẵn trong mã nguồn.

### 1. Chuẩn bị môi trường (Prerequisites)
Bạn cần cài đặt Java (JDK 17) và tải Android SDK.
Mở Terminal trên Ubuntu và cài đặt JDK 17 bằng dòng lệnh:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```
*(Lưu ý: Bạn cũng cần thiết lập biến môi trường `ANDROID_HOME` trỏ tới thư mục chứa Android SDK của bạn).*

### 2. Cấp quyền thực thi cho Gradle Wrapper
Mở Terminal, di chuyển (cd) vào thư mục gốc của dự án vừa giải nén và chạy lệnh sau để cấp quyền thực thi cho file `gradlew`:
```bash
chmod +x gradlew
```

### 3. Lệnh Build APK
Để tạo APK, bạn chạy công cụ `gradlew` ngay trong Terminal:

**Tạo bản Debug APK (Dùng để test):**
```bash
./gradlew assembleDebug
```
Sau khi dòng lệnh chạy thành công (`BUILD SUCCESSFUL`), file cài đặt sẽ nằm ở: 
`app/build/outputs/apk/debug/app-debug.apk`

**Tạo bản Release APK (Bản chính thức):**
```bash
./gradlew assembleRelease
```
File apk sẽ nằm ở: 
`app/build/outputs/apk/release/app-release-unsigned.apk` (Lưu ý nó sẽ là bản unsigned, bạn cần thiết lập cấu hình ký (signing tools) trong `build.gradle` hoặc dùng công cụ `apksigner` của Android SDK để ký nếu muốn đưa lên thiết bị thật hoặc store).

Nếu bạn muốn dọn dẹp các file cache của lần build cũ trước khi build mới:
```bash
./gradlew clean assembleDebug
```
