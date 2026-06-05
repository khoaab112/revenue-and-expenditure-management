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

## Hướng dẫn cài đặt và xuất file trực tiếp (File APK)

Để lấy mã nguồn và tạo ra file cài đặt `.apk` nhanh chóng, bạn hãy thực hiện theo đúng 4 bước cơ bản sau:

1. **Tải mã nguồn về máy (Clone):** Clone dự án từ Github hoặc tải file `.zip` mã nguồn về và giải nén.
2. **Giải mã file Keystore (Rất quan trọng):** 
   - Mở Terminal hoặc Command Prompt (cmd) ngay tại thư mục gốc của dự án.
   - Chạy dòng lệnh sau để giải mã file keystore, giúp gradle có thể build được:
     - Trên Windows (cmd/PowerShell): `certutil -decode debug.keystore.base64 debug.keystore`
     - Trên MacOS / Linux (Terminal): `base64 --decode debug.keystore.base64 > debug.keystore`
3. **Mở dự án:**
   - Khởi động Android Studio.
   - Chọn **Open** (hoặc `File -> Open...`), sau đó điều hướng đến thư mục dự án vừa clone và chờ Android Studio đồng bộ xong Gradle.
4. **Build lấy file APK:**
   - Trên thanh menu của Android Studio, chọn **Build** -> **Generate Signed Bundle / APK...**
   - Chọn **APK** rổi bấm **Next**.
   - (Bạn có thể tạo key mới hoặc để cấu hình mặc định) tiếp tục bấm **Next** và chọn **Create**. 
   - Khi hoàn tất, một thông báo sẽ hiện ở góc dưới bên phải màn hình. Bấm chữ **locate** trên thông báo đó để mở thư mục, ở đó sẽ có file `.apk` cài đặt để bạn sao chép thẳng vào điện thoại.

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
