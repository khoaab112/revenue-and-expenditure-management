# Tài liệu Kiến trúc và Luồng Logic (Architecture & Logic Flow)

Tài liệu này đóng vai trò là "bản đồ" của dự án **Revenue and Expenditure Management** (Quản lý chi tiêu). Nó giúp các developer (và cả AI) dễ dàng nắm bắt cấu trúc, nguyên lý hoạt động, và luồng dữ liệu của ứng dụng trong mỗi phiên làm việc mới.

## 1. Kiến trúc Tổng quan
Dự án được xây dựng theo mô hình **MVVM (Model - View - ViewModel)** kết hợp với **Clean Architecture** (cơ bản) sử dụng **Jetpack Compose** cho UI.
*   **Ngôn ngữ:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Local Database:** Room Database
*   **Asynchronous:** Coroutines & Flow

### Cấu trúc Thư mục chính (`app/src/main/java/com/app`)
*   **`ui/`**: Chứa toàn bộ giao diện người dùng.
    *   **`screens/`**: Các màn hình chính (Dashboard, History, Wallets, AddTransaction, v.v.).
    *   **`components/`**: Các UI component dùng chung (Dialog, Button, ProgressIndicator, v.v.).
    *   **`theme/`**: Định nghĩa màu sắc, typography và theme của ứng dụng.
*   **`data/`**: Chứa logic xử lý dữ liệu và cấu trúc database.
    *   `Entities.kt`: Các bảng (table) trong cơ sở dữ liệu (Wallet, Transaction, Event, Budget, SavingsGoal, Debt, v.v.).
    *   `FinanceDao.kt`: Data Access Object, chứa các câu truy vấn (SQL) tương tác với Room.
    *   `FinanceRepository.kt`: Lớp Repository đứng giữa ViewModel và DAO, đóng vai trò cung cấp dữ liệu cho app và trừu tượng hóa nguồn dữ liệu.
    *   `AppDatabase.kt`: Cấu hình Room database.
*   **`service/`**: Các background service và worker.
    *   `BankNotificationListenerService.kt` / `NotificationParser.kt`: Lắng nghe và bóc tách thông báo ngân hàng.
    *   `CloudSyncWorker.kt`: Đồng bộ dữ liệu lên cloud (Google Drive).
    *   `GeminiAdvisorService.kt`: Tích hợp AI tư vấn tài chính.

---

## 2. Luồng Logic Chạy (Data & Execution Flow)

### Luồng Dữ liệu chung (State Management)
Toàn bộ trạng thái (State) của ứng dụng được quản lý tập trung tại **`FinanceViewModel`** (kích thước khá lớn).
1.  **View (Compose)** gọi các Event/Action thông qua `FinanceViewModel`.
2.  **ViewModel** tiếp nhận Event, gọi tới **`FinanceRepository`** để xử lý logic business hoặc tương tác với database (Room).
3.  **Repository** gọi **`FinanceDao`** thực thi thao tác CRUD (Create, Read, Update, Delete).
4.  Dữ liệu trả về (thường dưới dạng `Flow` hoặc `suspend function`) được update vào các biến `StateFlow` trong `ViewModel`.
5.  **View (Compose)** observe (lắng nghe) các `StateFlow` này và tự động re-compose (vẽ lại) giao diện khi dữ liệu thay đổi.

### Luồng Thêm Giao Dịch (Add Transaction)
1. Người dùng nhập thông tin (Số tiền, Danh mục, Ví, Ghi chú) tại `AddTransactionScreen`.
2. Bấm "Lưu" -> Gọi hàm trong `FinanceViewModel` (ví dụ: `addTransaction()`).
3. ViewModel lấy số dư hiện tại của Ví từ Repository, tính toán lại số dư mới (cộng/trừ tùy loại giao dịch).
4. Cập nhật số dư Ví (`updateWallet`) và Thêm Giao dịch (`insertTransaction`) vào database thông qua một **Transaction (của database)** để đảm bảo tính toàn vẹn dữ liệu.
5. Nếu giao dịch liên quan đến Ngân sách (Budget) hoặc Mục tiêu tiết kiệm (Savings), hệ thống cũng sẽ tự động tính toán và cập nhật lại tiến độ.

### Luồng Đọc Thông báo Ngân hàng
1. Ứng dụng xin quyền truy cập thông báo (Notification Listener).
2. Khi có thông báo mới, `BankNotificationListenerService` bắt được.
3. Gửi thông tin sang `NotificationParser` để bóc tách (dùng regex hoặc logic parse để lấy số tiền, loại giao dịch, nội dung).
4. Lưu tạm vào một danh sách chờ.
5. Người dùng vào `BankNotificationHistoryScreen` để xác nhận (Approve) và lưu chính thức thành một Giao dịch trong ứng dụng.

## 3. Quy tắc Code (Coding Rules)
*(Phần này AI sẽ đọc để không phá vỡ logic cũ)*
*   **Giao diện:** Phải giữ nguyên `TopAppBar` đã chuẩn hóa (có shadow, chữ đậm 20sp). Không dùng hard-code màu (HEX) mà phải gọi từ `MaterialTheme.colorScheme`.
*   **Trạng thái (State):** Không giữ state rời rạc trong các màn hình nếu nó ảnh hưởng đến toàn hệ thống. Mọi State thay đổi dữ liệu chính cần được đưa về `FinanceViewModel`.
*   **BottomSheet:** Luôn dùng `skipPartiallyExpanded = true` để tránh lỗi kẹt UI.
*   **Tái sử dụng:** Tích cực sử dụng lại các components trong `ui/components/`.

## 4. Ghi chú cho các phiên làm việc tiếp theo
Bất cứ khi nào thêm màn hình hoặc bảng dữ liệu mới:
1. Thêm Model vào `Entities.kt`.
2. Định nghĩa câu query trong `FinanceDao.kt`.
3. Viết hàm wrap trong `FinanceRepository.kt`.
4. Tạo State và Event xử lý trong `FinanceViewModel.kt`.
5. Thiết kế UI trong `ui/screens/` và kết nối với ViewModel.

---

## 5. Đặc tả Chuẩn Logic Tài chính & Bảo toàn Dữ liệu
Đối với các logic nghiệp vụ phức tạp như **Phân tích Chi tiêu Thông minh (AI Advisor)**, **Chuẩn đoán rủi ro dòng tiền**, và **Quy chuẩn Bảo toàn Dữ liệu (Backup/Restore không mất dữ liệu)**, vui lòng xem tài liệu đặc tả chuyên sâu:
👉 **[SMART_FINANCIAL_LOGIC_SPECS.md](file:///d:/coding/revenue-and-expenditure-management/SMART_FINANCIAL_LOGIC_SPECS.md)**
*(Mọi Unit Test, test case, và nâng cấp thuật toán bắt buộc phải đối chiếu theo tài liệu này).*

