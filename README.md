# TextStoryReader

**TextStoryReader** là ứng dụng đọc truyện trên Android, được xây dựng bằng **Kotlin + Jetpack Compose**. Ứng dụng tập trung vào việc quản lý một thư viện truyện cục bộ và đọc các tệp **EPUB** và **TXT**, với mục tiêu xử lý thư viện lớn, lưu tiến độ đọc và cung cấp giao diện đọc có thể tùy biến.

> **Phiên bản:** `1.0.0`  
> **Nền tảng:** Android 7.0+ (API 24+)  
> **Application ID:** `com.tdpro1612.textstoryreader`

## ✨ Tính năng

### 📚 Thư viện truyện

- Chọn thư mục truyện thông qua **Android Storage Access Framework (SAF)**.
- Quét cả thư mục con để tìm các tệp được hỗ trợ.
- Hỗ trợ:
  - `.epub` (chính)
  - `.txt`
- Hiển thị tên truyện, loại tệp, dung lượng và tiến độ đọc.
- Phân trang thư viện, mặc định **20 truyện/trang**.
- Tìm kiếm theo **tên truyện hoặc tác giả**.
- Hiển thị **20 truyện đọc gần đây**.
- Có thể xóa truyện:
  - Chỉ xóa khỏi thư viện của ứng dụng.
  - Xóa cả tệp gốc khỏi bộ nhớ.
- Có chức năng quét lại thư mục đã chọn.
- Chọn lại thư mục, ứng dụng đối chiếu các tệp hiện có với cơ sở dữ liệu để cập nhật sách mới/thay đổi và loại bỏ các mục không còn tồn tại.
- Khi quét lại, ứng dụng xóa sạch từ đầu và quét lại từ đầu tạo lại database.

### 📖 Trình đọc EPUB

- Đọc nội dung EPUB trực tiếp từ thư viện.
- EPUB được giải nén vào cache ứng dụng để hỗ trợ truy cập nội dung theo từng chương.
- Phân tích mục lục EPUB với cơ chế **Fast Path / Deep Scan**.
- Có fallback khi cấu trúc mục lục của EPUB không hoàn chỉnh.
- Đọc và chuyển HTML trong EPUB thành văn bản thuần.
- Loại bỏ các thành phần không cần thiết như `script`, `style`, `head`, `nav`, `footer`, `header`, `iframe` và một số danh sách mục lục.
- Xử lý `<br>` và các block HTML để giữ lại cấu trúc đoạn văn khi chuyển sang text.
- Có cache nội dung HTML đã đọc nhằm giảm việc đọc lại cùng một tệp trong quá trình chuyển chương.

### 📄 Trình đọc TXT

- Tự nhận diện các tiêu đề chương phổ biến như:
  - `Chương`
  - `Chapter`
  - `Quyển`
  - `Tập`
  - `Hồi`
  - `Bài`
  - `Phần`
- Hỗ trợ số chương dạng **Ả Rập** và **La Mã**.
- Nếu TXT không có cấu trúc chương phù hợp, ứng dụng sử dụng cơ chế **fallback chunking**, chia nội dung thành các phần khoảng **15.000 ký tự** để tạo mục lục ảo.
- Nội dung được đọc theo `InputStream`, thay vì bắt buộc nạp toàn bộ file vào một lần.

### 🔖 Tiến độ đọc

- Lưu:
  - Chương đang đọc.
  - Vị trí trong chương.
  - Phần trăm tiến độ toàn bộ truyện.
  - Thời điểm đọc gần nhất.
- Khi mở lại truyện, ứng dụng khôi phục chương và tiến độ đã lưu.
- Chuyển chương bằng nút **Chương trước / Chương sau**.
- Có mục lục dạng drawer để nhảy trực tiếp tới chương.

### 🌓 Chế độ đọc

Hai chế độ đọc được hỗ trợ:

- **Cuộn dọc (Continuous Scroll)**
- **Lật trang (Page Flip)**

Ở chế độ lật trang, nội dung được tính toán thành các trang dựa trên kích thước vùng hiển thị, cỡ chữ và kiểu chữ hiện tại.

### 🎨 Tùy biến giao diện đọc

Ứng dụng cung cấp nhiều preset giao diện, bao gồm các nhóm sáng, dịu mắt và tối/AMOLED:

- Sáng
- Tuyết
- Giấy Cổ
- Kem Ấm
- Xanh Rêu
- Bạc Hà
- Biển Dịu
- Oải Hương
- Xám Tối
- Đêm Thẫm
- Than Hoạt Tính
- Đen Tuyền

Ngoài ra có 4 lựa chọn font:

- Mặc định hệ thống
- Serif
- Sans-Serif
- Monospace

Cỡ chữ hiện tại có thể điều chỉnh trong khoảng **12–32sp**.

Ứng dụng cũng hỗ trợ tùy chọn **giữ màn hình luôn sáng** khi đọc và có chức năng khôi phục toàn bộ cài đặt về mặc định.

## 🏗️ Kiến trúc tổng quan

Project được tổ chức theo các nhóm chức năng chính:

```text
app/src/main/java/com/tdpro1612/textstoryreader/
├── database/
│   ├── AppDatabase.kt
│   ├── BookDatabaseQueries.kt
│   ├── BookEntity.kt
│   ├── ChapterEntity.kt
│   ├── BookmarkEntity.kt
│   └── BookStatus.kt
│
├── manager/
│   ├── BookManager.kt
│   ├── BookCacheManager.kt
│   └── SettingsManager.kt
│
├── reader/
│   ├── BookContentReader.kt
│   ├── BookChapter.kt
│   ├── ReaderFactory.kt
│   ├── TxtContentReader.kt
│   ├── EpubContentReader.kt
│   ├── HtmlToTextParser.kt
│   └── epub/
│       ├── EpubFileReader.kt
│       └── EpubUnzipper.kt
│
├── scanner/
│   ├── BookScanner.kt
│   ├── BookScanManager.kt
│   ├── BookParserFactory.kt
│   └── ScanProgressState.kt
│
├── settings/
│   └── ReaderSettings.kt
│
└── ui/
    ├── library/
    ├── reader/
    └── settings/
```

### Database

Ứng dụng sử dụng **Room** để quản lý dữ liệu cục bộ.

Các bảng chính:

- `books`: thông tin truyện và tiến độ đọc.
- `book_chapters`: danh sách chương đã phân tích.
- `bookmarks`: cấu trúc dữ liệu cho dấu trang.

### Scanner

Luồng quét sách được quản lý bằng coroutine trên `Dispatchers.IO`, có trạng thái tiến trình để UI hiển thị số lượng file đã xử lý.

### EPUB cache

EPUB được copy và giải nén vào `cacheDir` của ứng dụng. Cache được đặt tên theo URI của EPUB để có thể tái sử dụng khi mở lại sách, tránh phải giải nén lại nếu cache vẫn còn tồn tại.

Quá trình giải nén sử dụng **Apache Commons Compress** và có kiểm tra đường dẫn nhằm hạn chế nguy cơ **Zip Slip**.

## 🧰 Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin 2.4.10 |
| UI | Jetpack Compose + Material 3 |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.4.1 |
| Compile SDK | 37 |
| Target SDK | 35 |
| Min SDK | 24 |
| Java | 17 |
| Database | Room 2.8.4 |
| State/Async | Kotlin Coroutines + Flow |
| EPUB | epublib |
| HTML parser | jsoup |
| ZIP/EPUB extraction | Apache Commons Compress |
| Persistent settings | AndroidX DataStore Preferences |
| Paging dependencies | AndroidX Paging |

## 🚀 Build project

### Yêu cầu

- Android Studio phiên bản hỗ trợ Android Gradle Plugin 9.2.x.
- JDK 17.
- Android SDK 37.
- Kết nối Internet ở lần build đầu tiên để Gradle tải dependency.

### Clone repository

```bash
git clone https://github.com/Tdpro1612/TextStoryReader_v2.git
cd TextStoryReader_v2
```

### Build Debug APK

Linux/macOS:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK debug sẽ được tạo trong:

```text
app/build/outputs/apk/debug/
```

### Build Release APK

Linux/macOS:

```bash
./gradlew assembleRelease
```

Windows:

```powershell
.\gradlew.bat assembleRelease
```

APK release sẽ nằm trong:

```text
app/build/outputs/apk/release/
```

> Bản release hiện tại đang để `isMinifyEnabled = false` và chưa cấu hình signing riêng trong project. Nếu phát hành lên Google Play hoặc phân phối production, cần bổ sung release signing phù hợp.

## 📱 Cách sử dụng

1. Mở **TextStoryReader**.
2. Chọn biểu tượng **thư mục** ở màn hình Thư viện.
3. Chọn thư mục chứa truyện ( lưu ý thư mục chứa không nên quá 5000 file thuần, nếu nhiều hơn thì nên bỏ vào thư mục con vì vẫn tự quét vào trong được. Nếu thư mục chứa quá lớn chẳng hạn 10k thì có thể bị treo do android không thể load ( không phải do app )).
4. Cấp quyền truy cập thư mục cho ứng dụng.
5. Ứng dụng sẽ quét thư mục và các thư mục con để tìm `.epub` và `.txt`.
6. Chọn một truyện để bắt đầu đọc.
7. Trong màn hình đọc:
   - Mở **Mục lục** để chuyển chương.
   - Mở **Cài đặt** để đổi chế độ đọc, theme và font.
   - Sử dụng nút chương trước/sau để điều hướng.

Ứng dụng sử dụng **Storage Access Framework** nên không cần yêu cầu quyền truy cập toàn bộ bộ nhớ theo kiểu legacy storage permission.

## ⚠️ Một số giới hạn hiện tại

- Phiên bản `1.0.0` tập trung vào các chức năng đọc EPUB/TXT và quản lý thư viện cơ bản.
- Dữ liệu bookmark và favorite đã có lớp database làm nền tảng, nhưng giao diện quản lý tương ứng chưa được hoàn thiện trong UI hiện tại.
- Cỡ chữ hiện được giới hạn ở `12–32sp` trong màn hình Settings.
- Chức năng tìm kiếm cho phép tìm truyện trong Library hiện tại theo tên truyện.

## 🔒 Quyền riêng tư

TextStoryReader được thiết kế để đọc các file sách do người dùng chọn thông qua Android Storage Access Framework. Dữ liệu thư viện và tiến độ đọc được lưu cục bộ trên thiết bị.

Project hiện không có thành phần backend/server riêng trong mã nguồn.

## 📌 Roadmap

Một số hướng phát triển có thể tiếp tục:

- Hoàn thiện UI cho bookmark.
- Có thể thêm tính năng audio.

## 📄 License

Repository hiện **đã có kèm file `LICENSE`**.

---

**TextStoryReader**  
Android application for managing and reading local EPUB/TXT story collections.
