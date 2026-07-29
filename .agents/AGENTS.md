# QUY TẮC PHÁT TRIỂN DỰ ÁN (WORKSPACE RULES)

Các quy tắc dưới đây chỉ có tác dụng trong phạm vi dự án này và AI bắt buộc phải tuân thủ nghiêm ngặt trong mọi phiên làm việc:

## 1. Chuẩn hóa giao diện (UI Format)
- **Thiết kế Header (TopAppBar)**: Nếu không sử dụng `AppHeader` chung từ `MainActivity` mà phải viết `TopAppBar` riêng cho một màn hình, bắt buộc phải copy chính xác 100% style của `AppHeader`:
  - Bọc `TopAppBar` trong `Surface` với thông số: `modifier = Modifier.fillMaxWidth()`, `color = MaterialTheme.colorScheme.surface`, `shadowElevation = 3.dp`, `tonalElevation = 1.dp`.
  - Chữ tiêu đề (Title) phải có style: `fontWeight = FontWeight.Black`, `fontSize = 20.sp`, `letterSpacing = 0.5.sp`, `color = MaterialTheme.colorScheme.onSurface`.
- **Màu sắc & Chủ đề**: Phải luôn ưu tiên tận dụng các biến màu từ `MaterialTheme.colorScheme` để tương thích tốt với Dark/Light Mode. Hạn chế tối đa việc hard-code mã màu HEX (ngoại trừ được người dùng cung cấp thiết kế cụ thể bằng ảnh/Figma với chỉ định chính xác).

## 2. Cấu trúc Code
- Giữ nguyên các hàm, comment và kiến trúc codebase hiện có, không tự ý refactor cấu trúc cốt lõi nếu không có yêu cầu.
- **Tái sử dụng Component**: Bắt buộc ưu tiên sử dụng lại các hàm, UI component và widget đã được xây dựng sẵn trong dự án (ví dụ: các thẻ Card, button, dialog dùng chung) thay vì tạo mới hoàn toàn để đảm bảo tính đồng nhất.

## 3. Quy tắc Component cụ thể
- **ModalBottomSheet**: Bắt buộc 100% tất cả các màn hình khi hiển thị ModalBottomSheet phải sử dụng duy nhất component khung chung `AppModalBottomSheet` (`com.app.ui.components.AppModalBottomSheet`). Cấu trúc luôn chia làm 3 phần chuẩn:
  - **Header**: Tiêu đề (Title `20.sp`, `FontWeight.Black`), Action đóng `(X)` cố định + Action mở rộng tùy chọn (ví dụ icon Info `i`), đường kẻ phân cách `HorizontalDivider`.
  - **Content**: Phần nội dung cuộn linh hoạt tùy theo nhu cầu từng màn hình.
  - **Footer**: Khu vực chứa nhóm Action chính (Button Hủy / Thực thi), bỏ trống hoặc chứa mô tả phụ tùy theo màn hình.
  - Luôn khởi tạo `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

## 4. Quy tắc Validation Form (Form Validation Rules)
- Khi thực hiện kiểm tra tính hợp lệ (validation) trên các ô nhập liệu (Input Form):
  - **Hiển thị lỗi trực tiếp**: Không âm thầm bỏ qua hay chỉ thông báo chung chung. Bắt buộc hiển thị thông báo lỗi rõ ràng ngay bên dưới ô input bị lỗi (`supportingText = { Text("...", color = MaterialTheme.colorScheme.error) }`).
  - **Nổi bật ô input lỗi**: Đánh dấu ô input sai bằng thuộc tính `isError = true` (viền đỏ nổi bật) để người dùng dễ dàng nhận biết và chỉnh sửa ngay lập tức.

## 5. Quy tắc Quản lý Hoạt ảnh & Re-render (Animation & Re-render Protection Rules)
- **Bảo vệ màn cũ (Prevent Re-animation on Navigation to Sub-screens)**: Các hiệu ứng chuyển động/xuất hiện (staggered entrance, chart path drawing, count-up percentage, list item animations) CHỈ ĐƯỢC RENDER/ANIMATE 1 LẦN duy nhất khi người dùng vào màn hình lần đầu hoặc khi dữ liệu (data/filter) thực sự có thay đổi.
- Khi người dùng điều hướng từ màn hình chính sang các màn hình con (sub-screens / sub-destinations không thuộc 5 tab menu chính) hoặc mở các Dialog/BottomSheet rồi back về màn hình cũ, các hiệu ứng KHÔNG ĐƯỢC tự động chạy lại nếu dữ liệu màn đó không đổi. Sử dụng `rememberSaveable`, `hasAnimated` flag hoặc `seenKeys` để ghi nhớ và bỏ qua animation khi back lại màn cũ.

## 6. Quy tắc Commit Message (Git Commit Rules)
- **Định dạng chuẩn**: Bắt buộc tạo commit message theo cấu trúc:
  `<branch> (<type>): <message>`
- **Yêu cầu đối với `<message>`**:
  - Bắt buộc viết bằng **Tiếng Anh (English)**.
  - Phải ngắn gọn, tối ưu nằm trên **1 dòng duy nhất**.
  - Tóm tắt đúng trọng tâm, không viết quá chi tiết hay dài dòng.
- **Các `<type>` thường dùng**: `feat` (tính năng), `fix` (sửa lỗi), `docs` (tài liệu), `style` (giao diện/format), `refactor` (tối ưu code), `chore` (cấu hình/thư viện).
- **Ví dụ**:
  - `main (feat): add savings vault management screen`
  - `feature/report (fix): fix total revenue calculation bug`




