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
- **ModalBottomSheet**: Khi sử dụng `ModalBottomSheet`, luôn khởi tạo state với tham số `skipPartiallyExpanded = true` (ví dụ: `rememberModalBottomSheetState(skipPartiallyExpanded = true)`) để BottomSheet mở rộng hoàn toàn, tránh tình trạng bị kẹt ở giữa màn hình gây khó chịu cho người dùng.

## 4. Quy tắc Validation Form (Form Validation Rules)
- Khi thực hiện kiểm tra tính hợp lệ (validation) trên các ô nhập liệu (Input Form):
  - **Hiển thị lỗi trực tiếp**: Không âm thầm bỏ qua hay chỉ thông báo chung chung. Bắt buộc hiển thị thông báo lỗi rõ ràng ngay bên dưới ô input bị lỗi (`supportingText = { Text("...", color = MaterialTheme.colorScheme.error) }`).
  - **Nổi bật ô input lỗi**: Đánh dấu ô input sai bằng thuộc tính `isError = true` (viền đỏ nổi bật) để người dùng dễ dàng nhận biết và chỉnh sửa ngay lập tức.

