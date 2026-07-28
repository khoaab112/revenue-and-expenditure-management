# Sơ Đồ Luồng Hoạt Động Dự Án (Project Flowcharts) - Revenue & Expenditure Management

Tài liệu này tổng hợp toàn bộ sơ đồ luồng (Flowchart) và biểu đồ kiến trúc của ứng dụng **Revenue and Expenditure Management** (Quản lý thu chi cá nhân). Các sơ đồ được vẽ bằng chuẩn [Mermaid](https://mermaid.js.org/) trực quan.

---

## 1. Sơ đồ Kiến trúc Tổng quan Hệ thống (System Architecture Diagram)

Ứng dụng được thiết kế theo mô hình **MVVM (Model - View - ViewModel)** kết hợp kiến trúc **Clean Architecture**:

```mermaid
flowchart TB
    subgraph UI_Layer ["Tầng Giao Diện (Jetpack Compose)"]
        UI_Dash["DashboardScreen (Bảng điều khiển)"]
        UI_Add["AddTransactionScreen (Thêm Giao dịch)"]
        UI_Hist["HistoryScreen và TimelineScreen"]
        UI_Wall["WalletsScreen và WalletManagementScreen"]
        UI_Budg["BudgetGoalScreen (Ngân sách và Tiết kiệm)"]
        UI_Debt["DebtBookScreen (Sổ nợ)"]
        UI_AI["AIAdvisorScreen (Trợ lý AI)"]
        UI_Noti["BankNotificationHistoryScreen"]
        UI_Set["SettingsScreen (Cài đặt, Backup/Restore)"]
        UI_Pin["PinLockScreen (Mật khẩu PIN)"]
        
        UI_Comp["Components: AppModalBottomSheet, CalculatorKeyboard, ColorPicker"]
    end

    subgraph Logic_Layer ["Tầng ViewModel và Services"]
        VM["FinanceViewModel (StateFlow Central)"]
        NotiService["BankNotificationListenerService"]
        NotiParser["NotificationParser"]
        AIService["GeminiAdvisorService"]
        SyncWorker["CloudSyncWorker"]
    end

    subgraph Data_Layer ["Tầng Dữ Liệu (Room Database)"]
        Repo["FinanceRepository"]
        DAO["FinanceDao"]
        DB[("AppDatabase (Room SQLite)")]
        Entities["Entities: Wallet, Transaction, Category, Budget, SavingsGoal, Debt, Event"]
    end

    subgraph External ["Dịch vụ Bên ngoài"]
        GeminiAPI["Google Gemini AI API"]
        GDrive["Google Drive REST API"]
        AndroidNoti["Android System Notification Manager"]
    end

    UI_Dash -->|User Event| VM
    UI_Add -->|User Event| VM
    UI_Hist -->|User Event| VM
    UI_Wall -->|User Event| VM
    UI_Budg -->|User Event| VM
    UI_Debt -->|User Event| VM
    UI_AI -->|User Event| VM
    UI_Noti -->|User Event| VM
    UI_Set -->|User Event| VM

    VM -->|StateFlow| UI_Dash
    VM -->|StateFlow| UI_Add
    VM -->|StateFlow| UI_Hist
    VM -->|StateFlow| UI_Wall
    VM -->|StateFlow| UI_Budg
    VM -->|StateFlow| UI_Debt
    VM -->|StateFlow| UI_AI
    VM -->|StateFlow| UI_Noti
    VM -->|StateFlow| UI_Set

    UI_Layer -.-> UI_Comp

    VM -->|Coroutines / Flow| Repo
    Repo -->|Data| VM
    VM --> AIService
    AIService --> VM

    AndroidNoti -->|Catch Notification| NotiService
    NotiService -->|Parse Text| NotiParser
    NotiParser -->|NotificationRecord| VM
    AIService -->|HTTP Prompt| GeminiAPI
    GeminiAPI -->|Response| AIService
    SyncWorker -->|OAuth2 / Backup JSON| GDrive

    Repo --> DAO
    DAO --> Repo
    DAO --> DB
    DB --> DAO
    DB --- Entities
```

---

## 2. Sơ đồ Quan hệ & Dòng Dữ liệu Thực thể (Entity Relationship Diagram)

```mermaid
erDiagram
    WALLET ||--o{ TRANSACTION : contains
    CATEGORY ||--o{ TRANSACTION : classifies
    EVENT ||--o{ TRANSACTION : attached_to
    DEBT ||--o{ TRANSACTION : repays
    SAVINGS_GOAL ||--o{ TRANSACTION : accumulates
    BUDGET ||--|| CATEGORY : limits

    WALLET {
        int id PK
        string name
        string type
        double balance
        string colorHex
        boolean isClosed
        double targetAmount
    }

    TRANSACTION {
        int id PK
        int walletId FK
        string walletName
        string type
        double amount
        string categoryName
        int eventId FK
        int debtId FK
        int destinationWalletId FK
        long timestamp
    }

    BUDGET {
        int id PK
        string categoryName
        double amountLimit
        int month
        int year
    }

    DEBT {
        int id PK
        string personName
        string type
        double totalAmount
        double remainingAmount
        string status
        long dueDate
    }

    SAVINGS_GOAL {
        int id PK
        string title
        double targetAmount
        double currentAmount
        long targetDate
    }

    EVENT {
        int id PK
        string name
        double budgetLimit
        boolean isCompleted
    }
```

---

## 3. Sơ đồ Luồng Thêm/Sửa Giao Dịch & Cập nhật Tự động (Add Transaction Flow)

```mermaid
flowchart TD
    Start([Người dùng mở màn hình Thêm Giao dịch]) --> InputData[Nhập số tiền, loại Thu/Chi/Chuyển khoản, chọn Ví, Danh mục, Ghi chú]
    InputData --> Validate{Kiểm tra dữ liệu hợp lệ?}
    
    Validate -- Không hợp lệ --> ShowErr[Hiển thị lỗi isError = true trên ô Input]
    ShowErr --> InputData
    Validate -- Hợp lệ --> Submit[Bấm nút 'Lưu Giao dịch']

    Submit --> VMCall[ViewModel: addTransaction / updateTransaction]
    VMCall --> DBTx[Bắt đầu Database Transaction]

    DBTx --> CheckType{Loại giao dịch là gì?}

    CheckType -- EXPENSE --> SubWallet[Trừ số dư Ví tương ứng]
    CheckType -- INCOME --> AddWallet[Cộng số dư Ví tương ứng]
    CheckType -- TRANSFER --> TransferWallet[Trừ tiền Ví nguồn, Cộng tiền Ví đích]

    SubWallet --> InsertTx[Lưu record vào bảng Transaction]
    AddWallet --> InsertTx
    TransferWallet --> InsertTx

    InsertTx --> CheckDebt{Có liên kết Khoản Nợ debtId?}
    CheckDebt -- Có --> UpdateDebt[Cập nhật remainingAmount và status của Khoản Nợ]
    CheckDebt -- Không --> CheckSavings

    UpdateDebt --> CheckSavings{Có liên kết Hũ Tiết Kiệm?}
    CheckSavings -- Có --> UpdateSavings[Cập nhật currentAmount của Hũ Tiết Kiệm]
    CheckSavings -- Không --> CheckBudget

    UpdateSavings --> CheckBudget{Có Hạn mức Ngân sách?}
    CheckBudget -- Có --> CalcBudget[Tính lại tổng chi trong tháng và Cảnh báo]
    CheckBudget -- Không --> Commit

    CalcBudget --> Commit[Commit Database Transaction thành công]
    Commit --> RefreshState[ViewModel cập nhật StateFlow]
    RefreshState --> End([Hoàn tất và Đóng Screen])
```

---

## 4. Sơ đồ Luồng Đọc & Bóc Tách Thông Báo Ngân Hàng (Bank Notification Scanning Flow)

```mermaid
flowchart TD
    Start([Thông báo Ngân hàng / SMS đẩy tới thiết bị]) --> Listener[BankNotificationListenerService bắt thông báo]
    Listener --> CheckFilter{Package Name có thuộc danh sách Bank App?}

    CheckFilter -- Không --> Ignore([Bỏ qua thông báo])
    CheckFilter -- Có --> ExtractText[Trích xuất Tiêu đề và Nội dung tin nhắn]

    ExtractText --> Parser[NotificationParser: Chạy Regex bóc tách dữ liệu]
    Parser --> MatchPattern{Khớp Mẫu Regex Số tiền và Tài khoản?}

    MatchPattern -- Không --> Ignore
    MatchPattern -- Có --> ConstructObj[Tạo đối tượng NotificationRecord]

    ConstructObj --> AntiDup{Check notificationKey đã tồn tại chưa?}
    AntiDup -- Đã trùng --> Ignore
    AntiDup -- Chưa trùng --> SavePending[Lưu vào danh sách chờ xác nhận Pending Notifications]

    SavePending --> UserNotify[Hiển thị thông báo trên App]
    UserNotify --> OpenScreen[Người dùng mở BankNotificationHistoryScreen]

    OpenScreen --> UserAction{Người dùng thao tác}
    UserAction -- Từ chối --> DeletePending[Xóa khỏi danh sách chờ] --> End([Kết thúc])
    UserAction -- Bỏ qua --> End
    UserAction -- Chấp nhận --> PreFill[Tự động điền dữ liệu sang AddTransactionScreen]

    PreFill --> UserConfirm[Người dùng kiểm tra và bấm Lưu]
    UserConfirm --> CallAddTx[Gọi Luồng Thêm Giao Dịch]
    CallAddTx --> UpdateStatus[Đánh dấu tin nhắn đã xử lý] --> End
```

---

## 5. Sơ đồ Bộ Máy AI Tư Vấn Tài Chính 3 Tầng & Gemini API (Context-Aware AI Advisor Engine)

```mermaid
flowchart TD
    Start([Khai thác Engine Phân tích 3 Tầng]) --> L1_1{Nợ đến hạn 7 ngày và Tiền khả dụng < Nợ?}
    
    L1_1 -- Có --> Risk1[🚨 Cảnh báo Nguy cơ thiếu hụt dòng tiền trả nợ]
    L1_1 -- Không --> L1_2{Tần suất chi 7 ngày cao và Cạn quỹ <= 5 ngày?}
    
    L1_2 -- Có --> Risk2[🚨 Cảnh báo Nguy cơ vỡ quỹ trong X ngày tới]
    L1_2 -- Không --> L1_3{Số ngày trong tháng >= 5 và Cạn tiền mặt <= 10 ngày?}
    
    L1_3 -- Có --> Risk3[⏳ Cảnh báo Kịch bản cạn tiền mặt]
    L1_3 -- Không --> L2_1{Danh mục sinh hoạt có tần suất phát sinh dày?}
    
    Risk1 --> L2_1
    Risk2 --> L2_1
    Risk3 --> L2_1
    
    L2_1 -- Có --> Rec1[💡 Khuyến nghị Tiết chế các khoản phát sinh linh hoạt]
    L2_1 -- Không --> L2_2{Danh mục bị vượt hạn mức ngân sách?}
    
    Rec1 --> L2_2
    L2_2 -- Có --> Rec2[📌 Khuyến nghị Thích ứng với hạng mục vượt mức]
    L2_2 -- Không --> L2_3{Hũ tiết kiệm đến hạn <= 30 ngày và Tiến độ < 80%?}
    
    Rec2 --> L2_3
    L2_3 -- Có --> Rec3[🎯 Khuyến nghị Tăng tốc nhịp độ tích lũy]
    L2_3 -- Không --> L3_1[So sánh chi tiêu Tuần lịch hiện tại với Tuần trước]
    
    Rec3 --> L3_1
    L3_1 --> L3_2[Phân loại Danh mục: Tích lũy/Nợ vs Tiêu sản sinh hoạt]
    L3_2 --> RenderLocal[Hiển thị Cảnh báo và Khuyến nghị quy chuẩn lên Dashboard]

    RenderLocal --> UserAskAI{Người dùng bấm 'Hỏi Trợ lý AI Gemini'?}
    UserAskAI -- Không --> End([Hoàn tất])
    UserAskAI -- Có --> BuildPrompt[GeminiAdvisorService: Thu thập 7 khối dữ liệu tài chính]

    BuildPrompt --> SendAPI[Gửi Prompt quy chuẩn lên Google Gemini API]
    SendAPI --> GeminiResp[Nhận câu trả lời phân tích chuyên sâu]
    GeminiResp --> DisplayAI[Hiển thị câu trả lời AI lên AIAdvisorScreen] --> End
```

---

## 6. Sơ đồ Luồng Bảo Toàn Dữ Liệu & Sao Lưu / Phục Hồi (Lossless Backup & Restore Flow)

```mermaid
flowchart TD
    B_Start([Người dùng bấm Sao lưu]) --> B_Export[Gọi FinanceRepository.exportAllDataAsJson]
    B_Export --> B_Moshi[Sử dụng Moshi Serialization trích xuất Table Entity]
    B_Moshi --> B_JSON[Tạo chuỗi JSON chuẩn hóa chứa 100% thuộc tính]
    B_JSON --> B_Target{Chọn nơi lưu?}
    B_Target -- Cục bộ --> B_SaveFile[Lưu file .json vào bộ nhớ máy]
    B_Target -- Google Drive --> B_Worker[CloudSyncWorker đẩy file .json lên Google Drive]
    B_SaveFile --> B_Done([Thông báo Sao lưu thành công])
    B_Worker --> B_Done

    R_Start([Người dùng chọn file JSON Phục hồi]) --> R_Read[Đọc chuỗi JSON từ File hoặc Google Drive]
    R_Read --> R_Parse[Parse JSON chuỗi sang Data Object với Restore Mapping Matrix]
    R_Parse --> R_Matrix[Áp dụng giá trị mặc định cho thuộc tính thiếu trong bản cũ]
    R_Matrix --> R_Wipe[Xóa dữ liệu cũ và Khôi phục dữ liệu theo Transaction]
    R_Wipe --> R_Verify[Kiểm tra liên kết Ví - Nợ - Giao dịch trọn vẹn]
    R_Verify --> R_Done([Thông báo Phục hồi thành công 100%])
```

---

## 7. Sơ đồ Luồng Khóa & Bảo Mật Ứng Dụng bằng Mã PIN (PIN Code Authentication Flow)

```mermaid
flowchart TD
    Start([Khởi động Ứng dụng]) --> CheckPin{Kiểm tra Cài đặt: Có bật Khóa PIN?}
    
    CheckPin -- Không --> OpenMain[Truy cập thẳng vào DashboardScreen] --> End([Sử dụng app bình thường])
    CheckPin -- Có --> LaunchPin[Hiển thị PinLockScreen]

    LaunchPin --> UserInput[Người dùng nhập Mã PIN 4 chữ số]
    UserInput --> CheckLength{Đã nhập đủ 4 số?}

    CheckLength -- Chưa --> UserInput
    CheckLength -- Đủ 4 số --> Verify{Khớp với PIN đã mã hóa?}

    Verify -- Sai PIN --> ShakeAnim[Hiển thị Hiệu ứng Rung viền đỏ và Thông báo sai]
    ShakeAnim --> ResetInput[Xóa các chữ số đã nhập] --> UserInput

    Verify -- Đúng PIN --> Unlock[Mở khóa thành công]
    Unlock --> OpenMain
```
