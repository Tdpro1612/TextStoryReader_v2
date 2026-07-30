## **TỔNG HỢP KIẾN TRÚC APP ĐỌC TRUYỆN OFFLINE (8 UI CHUẨN HÓA)**

### **UI 1: Thư viện (Library)**

Màn hình chính khi mở app.

* **Các nút/Yếu tố giao diện:**
    * Danh sách truyện đang có (Bìa sách, tên file, % tiến độ).
    * Thanh tìm kiếm truyện.
    * **Nút "Chọn thư mục" (Select Folder):** Đặt ngay góc trên màn hình. Bấm vào sẽ mở **UI 2** (Folderpicker) để đổi hoặc thêm thư mục truyện ngay lập tức.
    * Icon Bánh răng để vào **Cài đặt (UI 5\)**.

### **UI 2: Chọn Thư mục (Folder Picker \- Trình hệ thống)**

* **Cách hoạt động:** Khi bấm từ UI 1, app gọi công cụ Storage Access Framework của hệ điều hành. Người dùng chọn folder xong, app nhận quyền đọc tại đó và lập tức quay về UI 1 để quét và hiển thị truyện.

### **UI 3: Trình đọc truyện (Reader Screen)**

Không gian đọc sách tối giản. Khi chạm vào giữa màn hình sẽ hiện thanh công cụ:

* **Các nút/Yếu tố giao diện:**
    * Nút Back (quay lại Thư viện).
    * **Nút "Lưu dấu trang" (Add Bookmark):** Bấm phát là lưu ngay dòng/trang đang đọc vào danh sách.
    * **Nút "Mở Dấu trang" (View Bookmarks):** Bấm để nhảy sang **UI 6**.
    * **Nút "Mở Mục lục" (View Chapters):** Bấm để nhảy sang **UI 4**.
    * Thanh kéo tiến độ nhanh (Slider) ở đáy.

### **UI 4: Màn hình Danh sách chương (Chapter Screen)**

Màn hình chuyên biệt hiển thị mục lục của cuốn sách đang mở.

* **Các nút/Yếu tố giao diện:**
    * Danh sách hàng nghìn chương truyện (Chương đã đọc hiển thị màu xám mờ, chương đang đọc tô đậm).
    * **Thanh cuộn siêu tốc (Fast Scroller):** Ở cạnh phải màn hình để vuốt nhanh xuống các chương dưới cùng.
* **Cách hoạt động:** Bấm vào chương nào thì app đóng màn hình này lại và nhảy thẳng đến chương đó ở UI 3\.

### **UI 5: Cài đặt Cơ bản (Settings Screen)**

Nơi tinh chỉnh cấu hình hiển thị chung cho toàn app.

* **Các nút/Yếu tố giao diện:**
    * **Cỡ chữ (Font Size):** Nút \+ \- hoặc thanh trượt giới hạn từ **12sp** đến **30sp**.
    * **Theme mặc định:** 3 tone màu chuẩn (Sáng \- Tối \- Dịu mắt).
    * **Bảng màu tự chọn (Color Picker):** Ô màu kèm thanh kéo để chấm chọn chính xác màu nền và màu chữ theo ý thích.
    * **Mục mở rộng (Audio):** Lựa chọn giọng đọc TTS hệ thống (Nam/Nữ, Bắc/Nam).

### **UI 6: Màn hình Dấu trang (Bookmark Screen)**

Màn hình chuyên biệt quản lý các phần người dùng đã lưu lại của cuốn sách đó.

* **Các nút/Yếu tố giao diện:**
    * Danh sách các dấu trang đã lưu (Mỗi hàng hiển thị: Tên chương \+ Đoạn văn ngắn được lưu \+ Ngày giờ lưu).
    * Nút "Xóa" bên cạnh mỗi dòng để người dùng dọn dẹp các dấu trang cũ.
* **Cách hoạt động:** Bấm vào một dòng dấu trang, app sẽ đưa người dùng quay lại đúng vị trí dòng chữ đó ở UI 3\.

Dưới đây là mô tả chi tiết cho **UI 7 (Màn hình Lịch sử đọc)** và **UI 8 (Màn hình Tìm kiếm)** theo đúng cấu trúc và phong cách của UI 6:

### **UI 7: Màn hình Lịch sử đọc (History Screen)**

Màn hình chuyên biệt quản lý và theo dõi danh sách các cuốn sách/truyện người dùng đã mở đọc gần đây.

* **Các nút/Yếu tố giao diện:**
    * **Danh sách lịch sử:** Hiển thị 10 cuốn truyện đọc gần nhất. Mỗi dòng/thẻ bao gồm: Ảnh bìa (thumbnail), Tên truyện, Chương đang đọc dở (ví dụ: *Chương 45*), Phần trăm tiến độ (ví dụ: *68%*), và Thời gian đọc gần nhất (ví dụ: *10 phút trước*, *Hôm qua*).
    * **Nút "Xóa lịch sử" / "Xóa tất cả":** Nút nằm ở góc trên màn hình để xóa sạch danh sách lịch sử đọc.
    * **Nút "Xóa dòng" (icon thùng rác hoặc vuốt sang bên):** Giúp dọn dẹp riêng từng cuốn truyện khỏi danh sách lịch sử.
* **Cách hoạt động:**
    * Khi người dùng bấm vào bất kỳ cuốn truyện nào trong danh sách, app sẽ tự động nhảy đến **UI 3 (Reader Screen)** và khôi phục lại chính xác chương lẫn dòng chữ mà người dùng đang đọc dở.
    * Danh sách này tự động sắp xếp theo thứ tự thời gian giảm dần (`lastReadTime DESC`) — truyện vừa đọc xong sẽ luôn đứng ở vị trí đầu tiên.

### **UI 8: Màn hình Tìm kiếm (Search Screen)**

Màn hình giúp người dùng nhanh chóng tra cứu và lọc các cuốn truyện có trong Thư viện cá nhân.

* **Các nút/Yếu tố giao diện:**
    * **Thanh tìm kiếm (Search Bar):** Nút nhập văn bản kèm icon Kính lúp và nút "X" để xóa nhanh từ khóa vừa nhập.
    * **Bộ lọc nhanh (Filter Chips/Tags):** Các nút bấm nhanh bên dưới thanh tìm kiếm để lọc theo: *Thể loại* (Tiên hiệp, Võng du...), *Loại file* (.txt, .epub), hoặc *Trạng thái* (Đang đọc, Đã xong).
    * **Danh sách kết quả tìm kiếm:** Hiển thị các cuốn truyện khớp với từ khóa/bộ lọc (Mỗi dòng bao gồm: Ảnh bìa, Tên truyện, Tác giả, Thể loại/Tag, và Số chương/Dung lượng).
    * **Giao diện trống (Empty State):** Thông báo "Không tìm thấy truyện phù hợp" khi từ khóa nhập vào không khớp với dữ liệu nào.
* **Cách hoạt động:**
    * Người dùng gõ từ khóa (Tên truyện hoặc Tác giả). Hệ thống sẽ tự động lọc danh sách realtime (tìm kiếm tức thì khi đang gõ).
    * Khi bấm vào một cuốn truyện trong kết quả tìm kiếm:
        * Nếu truyện đã từng đọc ➔ Chuyển thẳng đến **UI 3 (Reader Screen)** tại vị trí đọc dở.
        * Nếu truyện mới chưa đọc ➔ Chuyển đến **UI 4 (Danh sách chương)** hoặc **UI 3** từ Chương 1\.

### **💡 Tóm tắt luồng di chuyển giữa các UI của Người dùng (User Flow)**

* Từ **Thư viện (UI 1\)** ➔ Đổi folder truyện qua **UI 2** hoặc Vào **Cài đặt (UI 5\)**.
* Từ **Thư viện (UI 1\)** ➔ Chọn sách vào **Trình đọc (UI 3\)**.
* Từ **Trình đọc (UI 3\)** ➔ Rẽ nhánh sang **Mục lục (UI 4\)** để chọn chương hoặc sang **Dấu trang (UI 6\)** để xem lại các phần đã lưu.

## **TỔNG HỢP LOGIC CHO TỪNG UI APP ĐỌC TRUYỆN**

### **🛠 LOGIC CHI TIẾT CHO UI 1: THƯ VIỆN (LIBRARY)**

Màn hình này có 3 nhiệm vụ logic chính cần xử lý:

#### **1\. Logic Khởi tạo & Quét File Tự động (onResume)**

* **Tình huống:** Khi người dùng vừa mở app, hoặc khi họ vừa tải truyện mới từ Chrome rồi bấm Back quay lại app.
* **Logic xử lý ngầm:**
    1. App kiểm tra xem trong Database local đã có "Đường dẫn thư mục được chỉ định" chưa.
        * *Nếu chưa có:* Hiển thị một giao diện trống kèm nút **"Chọn thư mục"** thật to ở giữa màn hình để nhắc nhở.
        * *Nếu đã có:* Kích hoạt một luồng quét ngầm (Background Thread).
    2. Luồng ngầm này **chỉ đọc danh sách tên file và ngày chỉnh sửa (**Last Modified**)** trong folder đó.
    3. So sánh với Database:
        * File nào có trên máy nhưng chưa có trong Database ➔ Thêm mới vào (Trạng thái: Chưa đọc, Tiến độ: 0%).
        * File nào có trong Database nhưng không còn trên máy (người dùng đã xóa file ngoài đời) ➔ Xóa khỏi Database để dọn rác.
        * File nào trùng khớp ➔ Giữ nguyên dữ liệu tiến độ đọc.
    4. Cập nhật lại giao diện (UI) để hiển thị danh sách mới nhất.

#### **2\. Logic Hiển thị danh sách & Tiến độ đọc**

* **Sắp xếp (Sorting):** Mặc định, cuốn truyện nào có **thời gian đọc gần nhất (**Last Read Time**)** sẽ được đẩy lên đầu tiên để người dùng tiện bấm đọc tiếp. Các truyện còn lại sắp xếp theo bảng chữ cái A-Z của tên file.
* **Xử lý Bìa sách:**
    * Với file .epub: Code sẽ chạy ngầm giải nén file để tìm file ảnh bìa (thường là cover.jpg bên trong cấu trúc file epub) rồi hiển thị lên.
    * Với file .txt: Vì không có ảnh, app sẽ tự động render một ảnh bìa giả lập (Placeholder) với màu nền ngẫu nhiên và chèn Tên truyện lên đó cho đẹp mắt.

#### **3\. Logic Thanh Tìm Kiếm (Search Bar)**

* **Tình huống:** Người dùng gõ từ khóa để tìm truyện trong thư viện.
* **Logic xử lý:**
    * App sẽ lọc (Filter) trực tiếp trên danh sách truyện đang hiển thị trong bộ nhớ RAM thay vì truy vấn lại Database liên tục (giúp app không bị khựng khi gõ).
    * Tìm kiếm **không phân biệt chữ hoa, chữ thường** và **bỏ dấu tiếng Việt** (Ví dụ: Gõ "dac nhan tam" vẫn ra truyện "Đắc Nhân Tâm").

### **🛠 LOGIC CHI TIẾT CHO UI 2: CHỌN THƯ MỤC (FOLDER PICKER)**

Màn hình này sẽ xử lý các logic chính sau:

#### **1\. Logic Kích hoạt Trình chọn của Hệ điều hành (System Picker)**

* **Tình huống:** Người dùng bấm vào nút "Chọn thư mục" ở UI 1\.
* **Logic xử lý:**
    1. App không tự vẽ ra một cây thư mục (vì như thế vi phạm chính sách bảo mật của Android/iOS), mà sẽ phát một lệnh gọi (Intent trên Android hoặc Document Picker trên iOS) để **yêu cầu hệ điều hành mở trình quản lý file mặc định của máy lên**.
    2. Cấu hình lệnh gọi này ở chế độ: **Chỉ chọn Thư mục (Directory)**, không chọn file lẻ.

#### **2\. Logic Tiếp nhận và Lưu quyền truy cập Vĩnh viễn (Persistable URI Permission)**

* **Tình huống:** Người dùng chọn xong một thư mục (ví dụ: Download/Ebooks) và bấm "Cho phép/Allow". Hệ điều hành trả quyền về cho app.
* **Logic xử lý cực kỳ quan trọng:**
    1. **Xin quyền vĩnh viễn:** Mặc định, quyền hệ điều hành cấp cho app chỉ có tác dụng trong phiên làm việc đó (tắt app mở lại sẽ mất). Lập trình viên phải dùng lệnh takePersistingUriPermission để **khóa quyền này lại vĩnh viễn**. Từ đó về sau, dù máy có khởi động lại, app vẫn có quyền đọc folder này mà không cần xin lại.
    2. **Lưu đường dẫn:** Lưu chuỗi đường dẫn (URL string) của folder này vào bộ nhớ cài đặt nhanh của app (SharedPreferences hoặc UserDefaults).
    3. **Tự động quay về:** Đóng UI 2 và lập tức chuyển hướng người dùng quay lại UI 1\.

#### **3\. Logic Xử lý Ngoại lệ (Edge Cases \- Khi có lỗi)**

Để app không bị crash (văng ứng dụng), logic cần chặn trước các trường hợp sau:

* **Trường hợp Người dùng hủy chọn:** Họ mở lên nhưng bấm nút "Back" hoặc "Hủy" mà không chọn folder nào.
    * *Cách xử lý:* Kiểm tra nếu dữ liệu trả về bị rỗng (null), app sẽ hiển thị một thông báo nhẹ (Toast) kiểu: *"Bạn chưa chọn thư mục nào"* và giữ nguyên giao diện hiện tại, không làm gì thêm.
* **Trường hợp Thư mục bị xóa ngoài đời:** Người dùng đã chọn folder đó rồi, nhưng sau đó họ dùng app khác xóa mất cái folder đó trên điện thoại.
    * *Cách xử lý:* Khi UI 1 gọi đường dẫn này ra để quét mà không tìm thấy, app sẽ tự động xóa đường dẫn lỗi này trong bộ nhớ và hiển thị lại giao diện trống kèm nút "Chọn thư mục" để người dùng chọn lại nơi khác.

### **🛠 LOGIC CHI TIẾT CHO UI 3 \- TRÌNH ĐỌC TRUYỆN**

Màn hình này hoạt động theo cơ chế **Lật trang ngang (Page View)**, tối giản giao diện tối đa để tập trung vào trải nghiệm đọc, xử lý ngầm bằng các thuật toán tối ưu phần cứng.

#### **1\. Logic Chốt chặn & Phân đoạn khi mở File nặng (File Loading & Splitter)**

Đây là bộ lọc đầu tiên ngay khi người dùng bấm mở một cuốn truyện từ UI 1:

* **Trường hợp 1 (File đã có cấu trúc mục lục):** App nạp chương hiện tại được yêu cầu vào bộ nhớ đệm để xử lý.
* **Trường hợp 2 (File** .txt **thô, quá nặng, không có danh sách chương):** Để tránh việc nạp file dung lượng lớn (vài MB đến vài chục MB) gây đứng máy (ANR) hoặc tràn RAM, app kích hoạt **Hàm phân đoạn dữ liệu (Chapter Splitter)**:
    * App sẽ không đọc hết file mà dùng kỹ thuật RandomAccessFile để chia nhỏ file truyện thành các phân đoạn (ví dụ: cắt theo cấu hình dao động từ 3000-4000 từ, hoặc theo số lượng ký tự, sẽ định nghĩa sâu sau).
    * Thuật toán cắt sẽ thông minh tìm đến dấu xuống dòng (\\n) hoặc dấu chấm câu gần nhất ở ranh giới cắt để **không bao giờ chặt đôi một câu văn hoặc một từ**.
    * App chỉ nạp đúng phân đoạn mà người dùng đang đọc dở vào bộ nhớ để xử lý text và hiển thị, các phân đoạn khác nằm chờ dưới dạng file thô.

#### **2\. Luồng chuẩn hóa Văn bản đầu vào (Text Processing)**

* **Chốt chặn bảo vệ:** Phân đoạn text hoặc chương truyện sau khi được nạp vào bộ nhớ, bắt buộc phải chạy qua hàm xử lý normalizeVietnameseText (sử dụng java.text.Normalizer dạng NFC).
* **Mục đích:** Trị dứt điểm các lỗi font tiếng Việt kinh điển (như ký tự giả mạo Ð, ð, ñ, Ñ biến thành Đ, đ) trước khi đưa vào bộ render, đảm bảo chữ hiển thị sạch sẽ 100%.

#### **3\. Logic Phân trang (Pagination) & Tiết kiệm RAM**

* **Cơ chế Recycler:** Để tiết kiệm tài nguyên, app sử dụng thành phần ViewPager2. Thành phần này chỉ render trực tiếp vào RAM đúng **3 trang** (Trang trước, Trang hiện tại, Trang kế tiếp). Các trang còn lại chỉ tồn tại dưới dạng text thô trong bộ nhớ tạm.
* **Thuật toán cắt trang hình học:** Dựa vào kích thước thực tế của màn hình và cấu hình hiển thị (Cỡ chữ giới hạn từ **12sp đến 30sp**, màu nền), app dùng StaticLayout để tính toán chính xác số dòng có thể chứa trong một trang. Thuật toán tự động chặt đứt chuỗi chữ khi chuẩn bị tràn cạnh đáy màn hình và cắt theo dấu cách để **không bao giờ bị vỡ từ/nửa từ**.

#### **4\. Logic Lưu và Phục hồi vị trí đọc dở (Bookmark tự động)**

* **Đơn vị lưu trữ:** App không lưu theo số trang (vì khi đổi cỡ chữ, tổng số trang sẽ thay đổi). App lưu theo **Mã chương/Mã phân đoạn (Chapter/Chunk ID)** và **Vị trí ký tự đầu trang (Character Offset Index)** vào Database local (SQLite).
* **Khi thoát app (**onPause**):** Lưu ngay lập tức vị trí ký tự của dòng trên cùng màn hình vào Database.
* **Khi mở lại sách:** App truy vấn vị trí ký tự đó, chạy thuật toán phân trang riêng cho chương/phân đoạn đó, tìm xem ký tự đó nằm ở trang mấy mới và nhảy thẳng đến trang đó trong vòng dưới 0.05 giây.

#### **5\. Logic Tái phân trang khi Xoay màn hình hoặc Đổi Cài đặt**

* **Tình huống:** Người dùng đổi cỡ chữ ở UI 5 hoặc xoay ngang/dọc điện thoại.
* **Logic xử lý:**
    1. Trước khi thay đổi có hiệu lực, app ghi nhớ nhanh **chỉ số ký tự đầu tiên** đang hiện trên màn hình.
    2. Áp dụng cấu hình hiển thị mới (Kích thước View mới hoặc Cỡ chữ mới).
    3. Chạy lại Thuật toán Cắt trang để tạo ra bộ trang mới.
    4. Dò tìm ký tự đã ghi nhớ đang nằm ở Trang số mấy trong bộ trang mới và cuộn ngay đến trang đó. Người dùng sẽ thấy chữ to lên hoặc màn hình xoay ngang nhưng dòng chữ họ đang đọc vẫn giữ nguyên, không bị mất dấu.

#### **6\. Logic Di chuyển Liên kết giữa các Chương/Phân đoạn (Seamless Transition)**

* **Vuốt tới phần mới:** Khi người dùng ở trang cuối cùng của Chương/Phần hiện tại và vuốt sang phải ➔ App tự động gọi Chương/Phần tiếp theo từ bộ nhớ (nếu là file thô thì nạp phân đoạn tiếp theo) ➔ Xử lý sạch chữ ➔ Phân trang ➔ Hiển thị ngay **Trang 1 của Chương/Phần mới**.
* **Lùi về phần cũ:** Khi ở trang đầu tiên của Chương/Phần hiện tại và vuốt ngược lại ➔ App tự động nạp Chương/Phần trước đó ➔ Xử lý chữ ➔ Phân trang ➔ Đưa người dùng đến thẳng **Trang cuối cùng của Chương/Phần trước đó**.

#### **7\. Logic Menu Ẩn/Hiện và Các nút tương tác**

* Mặc định màn hình ẩn toàn bộ thanh công cụ, chỉ hiện chữ.
* **Chạm vùng biên (Trái/Phải):** Thực hiện lệnh lật trang (Lùi/Tiến).
* **Chạm vùng trung tâm (30% giữa màn hình):** Kích hoạt hiển thị thanh công cụ bao gồm:
    * *Header:* Nút Quay lại Thư viện (UI 1), Nút **"Lưu dấu trang nhanh"** (Lưu vị trí dòng hiện tại vào Database), Nút mở màn hình Dấu trang (UI 6), Nút mở màn hình Mục lục (UI 4).
    * *Footer:* Thanh trượt tiến độ nhanh (Progress Slider) để người dùng kéo nhảy trang nhanh theo tỷ lệ % toàn tập truyện.
    * Chạm lại vùng giữa để ẩn menu, trả lại không gian đọc tối giản.

### **🛠 LOGIC CHI TIẾT CHO UI 4: DANH SÁCH CHƯƠNG (CHAPTER SCREEN)**

Màn hình này chịu trách nhiệm hiển thị mục lục (thật hoặc ảo) một cách nhanh chóng, mượt mà và chuyển hướng người dùng quay lại UI 3 một cách chính xác.

#### **1\. Logic Nhận diện & Nạp Mục lục (Mô hình 2 Nhánh xử lý)**

Khi người dùng bấm vào nút "Mục lục" từ UI 3, app sẽ không quét lại file từ đầu mà truy vấn trực tiếp cấu trúc đã được định hình trước đó trong Database:

* **Nhánh 1 (Truyện có chương hồi chuẩn):** App lấy danh sách tên chương được bẻ bằng Regex từ trước (Ví dụ: *Chương 1: Khởi đầu*, *Chương 2: Gặp gỡ*...) để nạp vào giao diện.
* **Nhánh 2 (Truyện file thô, quá nặng, đã kích hoạt hàm** chapter\_split **ở UI 3):** App sẽ gọi ra **Mục lục ảo** đã được thuật toán tự động cắt khúc theo số từ/ký tự từ trước. Các mục sẽ hiển thị dưới dạng: *Phần 1*, *Phần 2*, *Phần 3*... nhằm đảm bảo người dùng vẫn có các cột mốc để bấm nhảy phần đọc, không bị lạc giữa một file text mênh mông.

#### **2\. Logic Hiển thị Tối ưu bộ nhớ (UI Virtualization)**

* **Tình huống:** Gặp các bộ truyện tiên hiệp dài 3.000 đến 5.000 chương. Nếu nạp cả 5.000 dòng giao diện cùng lúc, app sẽ đứng hình (freeze) ngay lập tức.
* **Logic xử lý:** Danh sách phải được xây dựng dựa trên thành phần tối ưu hệ thống (RecyclerView trên Android hoặc UICollectionView trên iOS). Cơ chế này chỉ tạo và giữ trong RAM đúng số lượng dòng hiển thị vừa vặn màn hình (khoảng 10-15 dòng). Khi người dùng cuộn lên/xuống, app sẽ tái sử dụng các ô giao diện cũ và chỉ thay đổi ruột chữ bên trong, giúp cuộn mượt mà 100%.

#### **3\. Logic "Đang ở đâu, định vị ở đó" (Active Chapter Focus)**

* **Đánh dấu trạng thái:** App đối chiếu ID chương/phân đoạn hiện tại đang đọc dở ở UI 3 với danh sách mục lục. Dòng chương đó sẽ được **tô đậm hoặc đổi màu sắc nổi bật** để người dùng biết mình đang ở đâu.
* **Tự động cuộn (Auto-Scroll):** Ngay khi màn hình UI 4 vừa mở lên, app tự động thực hiện lệnh cuộn màn hình (scrollToPosition) để đưa chương đang đọc dở vào chính giữa tầm mắt người dùng, họ không cần phải tự vuốt tay tìm kiếm xem mình đang đọc đến đâu.
* **Trạng thái đã đọc:** Các chương nằm phía trước chương đang đọc sẽ được tự động làm mờ chữ đi (màu xám dịu) để biểu thị trạng thái đã đọc xong.

#### **4\. Logic Thanh cuộn siêu tốc (Fast Scroller)**

* Bên cạnh phải màn hình sẽ có một thanh trượt dọc siêu nhỏ chạy từ đỉnh đến đáy.
* **Cách hoạt động:** Khi người dùng đè ngón tay và kéo nhanh thanh này, danh sách chương sẽ nhảy cóc theo tỷ lệ phần trăm (ví dụ kéo đến giữa thanh là nhảy thẳng đến chương thứ 1.500 của bộ truyện 3.000 chương).
* **Tooltip thời gian thực:** Một bong bóng nhỏ (Tooltip) sẽ hiện nổi lên ngay cạnh ngón tay người dùng, hiển thị số chương/tên phần tương ứng theo thời gian thực khi họ đang rà tay, giúp họ buông tay ra đúng vị trí cần tìm.

#### **5\. Logic Điều hướng quay lại UI 3 (Navigation Callback)**

* Khi người dùng bấm vào một chương hoặc một phần bất kỳ trong danh sách:
    1. App ghi nhận **Vị trí ký tự bắt đầu** (Character Offset) hoặc ID của chương/phần đó.
    2. Đóng ngay lập tức UI 4 để quay lại UI 3\.
    3. Gửi lệnh điều hướng cho UI 3: *"Hãy nạp dữ liệu của chương/phần này, chạy hàm xử lý text NFC, phân trang lại và hiển thị ngay Trang 1 cho tôi"*.

### **🛠 LOGIC CHI TIẾT CHO UI 5: CÀI ĐẶT CƠ BẢN (SETTINGS SCREEN)**

Màn hình này có nhiệm vụ quản lý cấu hình hiển thị toàn hệ thống, lưu trữ trạng thái lâu dài và đồng bộ các thay đổi này tới Trình đọc (UI 3\) theo thời gian thực.

#### **1\. Logic Giới hạn Cỡ chữ (Font Size Boundary)**

* **Thông số cấu hình:** Nút \+ và \- (hoặc thanh trượt Slider).
* **Logic chốt chặn (Chí mạng):** Lập trình viên bắt buộc phải khóa cứng hai đầu giá trị: **Min: 12sp** và **Max: 30sp**.
    * *Nếu người dùng bấm giảm xuống dưới 12sp:* Nút \- tự động bị vô hiệu hóa (Disabled/Mờ đi), app không cho giảm tiếp để bảo vệ mắt người đọc khỏi bị mỏi.
    * *Nếu người dùng bấm tăng quá 30sp:* Nút \+ tự động bị vô hiệu hóa để chặn trước lỗi tràn chữ, vỡ layout, hoặc một trang hiển thị được quá ít từ làm gãy ngữ cảnh đọc.
* **Lưu trữ:** Mỗi lần nhấn \+/\-, giá trị mới (ví dụ: 18) sẽ được ghi đè ngay lập tức vào bộ nhớ nhanh SharedPreferences (Android) hoặc UserDefaults (iOS) dưới key current\_font\_size.

#### **2\. Logic Áp dụng Theme Mặc định (Preset Themes)**

App cung cấp sẵn 3 gói màu (Màu nền \+ Màu chữ) được tối ưu sẵn cho các môi trường ánh sáng khác nhau:

* **Theme Sáng:** Nền Trắng tinh (\#FFFFFF) / Chữ Đen tuyền (\#000000). Dùng khi đọc ban ngày, ngoài trời.
* **Theme Tối:** Nền Đen thuần (\#000000) / Chữ Xám dịu (\#888888). Dùng để đọc ban đêm chống lóa. (Không dùng chữ trắng tinh trên nền đen vì sẽ gây bóng ma cho mắt).
* **Theme Dịu mắt (Sepia):** Nền Vàng giấy cũ (\#F4ECD8) / Chữ Nâu đậm (\#4A3624). Mô phỏng trang sách thật, giảm tối đa ánh sáng xanh, khuyên dùng đọc thời gian dài.
* **Logic xử lý:** Khi chọn 1 trong 3 theme này, app sẽ tự động ghi đè mã màu nền và màu chữ tương ứng vào bộ nhớ cài đặt và cập nhật trực tiếp lên giao diện xem trước (Preview) nếu có.

#### **3\. Logic Bảng màu Tự chọn nâng cao (Custom Color Picker)**

Dành cho người dùng muốn cá nhân hóa sâu sắc màu sắc theo sở thích cụ thể.

* **Thành phần UI:** Một ô/vòng tròn bảng màu (Color Palette) kèm một thanh kéo sắc độ (Brightness/Saturation Slider).
* **Logic xử lý:**
    1. Người dùng chạm tay vào điểm nào trên bảng màu, app sẽ trích xuất ngay lập tức **Mã màu Hex** (Ví dụ: Nền \#E0F7FA, Chữ \#006064).
    2. App hiển thị một hộp thoại (Box) nhỏ ngay tại chỗ làm cấu trúc xem trước (Preview Text) chứa vài dòng chữ mẫu để người dùng thẩm định xem độ tương phản giữa màu nền và màu chữ họ vừa chọn có dễ đọc hay không.
    3. Khi họ bấm "Áp dụng", hệ thống mới tiến hành ghi đè hai mã màu tùy biến này vào Database và hủy kích hoạt các Theme mặc định.

#### **4\. Logic Chuẩn bị Tích hợp Audio (TTS \- Text-to-Speech)**

Thiết kế sẵn một khu vực chờ cho tính năng Audio trong tương lai để sau này không phải đập đi xây lại UI.

* **Thành phần UI:** Một mục chọn có tiêu đề **"Giọng đọc truyện (Audio TTS)"**.
* **Logic xử lý:**
    * Khi người dùng bấm vào, app sẽ thực hiện một lệnh gọi ngầm hệ thống (TextToSpeech.getVoices() trên Android) để lấy ra danh sách các engine giọng đọc ngôn ngữ Tiếng Việt mặc định có sẵn trên thiết bị (Ví dụ: Giọng Nam/Nữ miền Bắc, Giọng Nam/Nữ miền Nam của Google hoặc Apple).
    * Lưu ID giọng đọc được chọn vào cài đặt. Hiện tại, cài đặt này sẽ được lưu trữ sẵn sàng. Khi chúng ta phát triển tính năng Audio ở các phiên bản sau, Trình đọc (UI 3\) chỉ cần gọi ID giọng đọc này ra và chạy mà không cần thay đổi cấu trúc UI 5 nữa.

### **🛠 LOGIC CHI TIẾT CHO UI 6: MÀN HÌNH DẤU TRANG (BOOKMARK SCREEN)**

Màn hình này chịu trách nhiệm hiển thị dữ liệu dấu trang từ Database, cho phép người dùng dọn dẹp (xóa) và thực hiện lệnh "nhảy" chính xác về Trình đọc (UI 3).

#### **1\. Logic Truy vấn & Hiển thị Dữ liệu (Scoped Query)**

* **Phạm vi dữ liệu:** Khi mở UI 6 từ một cuốn truyện ở UI 3, app **chỉ truy vấn các dấu trang thuộc về chính cuốn truyện đó** dựa trên Book ID, không hiển thị trộn lẫn dấu trang của truyện khác.
* **Cấu trúc một hàng giao diện (Bookmark Item):** Mỗi dòng dấu trang hiển thị đầy đủ 3 thông tin cốt lõi:
    * *Tiêu đề:* Tên chương/phân đoạn tại thời điểm lưu (Ví dụ: "Chương 12" hoặc "Phần 4").
    * *Nội dung trích dẫn:* Đoạn văn ngắn (khoảng 50-100 ký tự đầu tiên tính từ vị trí lưu) để người dùng nhớ lại ngữ cảnh đoạn đó nói về cái gì.
    * *Mốc thời gian:* Ngày/giờ thực hiện bấm lưu (Ví dụ: 14:30 \- 18/07/2026).
* **Sắp xếp (Sorting):** Mặc định hiển thị theo thứ tự **vị trí xuất hiện từ đầu đến cuối truyện** (Chương nhỏ xếp trước, chương lớn xếp sau) để người dùng theo dõi mạch truyện một cách logic nhất.

#### **2\. Logic "Nhảy" về Vị trí cũ (Deep-Link Navigation)**

* **Tình huống:** Người dùng lướt danh sách dấu trang và bấm chọn vào dòng "Đoạn văn hay ở Chương 5".
* **Logic xử lý ngầm:**
    1. App trích xuất 2 thông số chí mạng được gắn kèm dòng dấu trang đó: **ID chương/phân đoạn** và **Vị trí ký tự chính xác (Character Offset)**.
    2. Đóng lập tức UI 6 để quay lại UI 3\.
    3. Phát lệnh điều hướng xuyên thấu cho UI 3: *"Hãy load ngay chương này, nạp text qua hàm chuẩn hóa Unicode NFC, chạy phân trang nhanh, và dùng lệnh* scrollTo *đưa trang sách chứa ký tự này lên màn hình"*. Người dùng sẽ thấy mình được dịch chuyển tức thời về đúng dòng chữ ngày xưa họ từng đánh dấu.

#### **3\. Logic Xóa Dấu trang (Dọn dẹp bộ nhớ)**

* **Thao tác UI:** Bên cạnh mỗi dòng dấu trang có một biểu tượng Thùng rác nhỏ (hoặc người dùng có thể vuốt ngang dòng đó \- Swipe to Delete).
* **Logic xử lý:**
    * Khi người dùng bấm xóa, app sẽ xóa bản ghi đó khỏi bảng Bookmarks trong Database local.
    * **Tối ưu trải nghiệm:** Áp dụng hiệu ứng xóa dòng mịn màng (notifyItemRemoved trong Kotlin RecyclerView) để danh sách tự động co lại, không làm màn hình bị chớp hay load lại từ đầu.

#### **4\. Logic Xử lý Giao diện Trống (Empty State)**

* **Tình huống:** Người dùng chưa từng bấm lưu bất kỳ dấu trang nào trong cuốn truyện này nhưng vẫn bấm vào UI 6\.
* **Logic xử lý:** Thay vì để một màn hình trắng trơn trông như app bị lỗi, hệ thống sẽ ẩn danh sách đi và hiển thị một ảnh minh họa vector nhẹ nhàng kèm dòng chữ hướng dẫn: *"Bạn chưa lưu dấu trang nào. Hãy bấm biểu tượng Bookmark khi đang đọc để lưu lại những đoạn văn hay nhé\!"*

### **🛠 LOGIC CHI TIẾT CHO UI 7: MÀN HÌNH LỊCH SỬ ĐỌC (HISTORY SCREEN)**

Màn hình này phụ thuộc trực tiếp vào các thông số lastReadTime và lastChapterIndex lưu trong bảng books (BookEntity).

#### **1\. Logic Truy vấn & Tự động Sắp xếp Danh sách (Query & Auto Sorting)**

* **Tình huống:** Người dùng mở màn hình UI 7 từ menu hoặc trang chủ.
* **Logic xử lý:**
    * **Lắng nghe luồng dữ liệu (StateFlow / LiveData):** DatabaseControl sẽ phát lệnh tới BookDao chạy câu lệnh SQL:  
      SELECT \* FROM books WHERE lastReadTime \> 0 ORDER BY lastReadTime DESC LIMIT 10
    * **Cập nhật Giao diện Realtime:** Kết quả trả về một danh sách tối đa 10 cuốn sách có thời gian đọc gần đây nhất (lastReadTime \> 0). Danh sách này tự động cập nhật ngay lập tức nếu có bất kỳ cuốn truyện nào vừa được mở đọc.
    * **Tính toán độ lùi thời gian (Time Ago Formatting):** Dựa vào giá trị lastReadTime (dạng miligiây timestamp), logic UI sẽ quy đổi ra chuỗi văn bản thân thiện với người dùng:
        * Chênh lệch $\< 1 \\text{ phút} \\rightarrow$ "Vừa xong"
        * Chênh lệch $\< 60 \\text{ phút} \\rightarrow$ "X phút trước"
        * Chênh lệch $\< 24 \\text{ giờ} \\rightarrow$ "X giờ trước"
        * Lớn hơn $24 \\text{ giờ} \\rightarrow$ Hiển thị ngày tháng (ví dụ: "22/05/2026").

#### **2\. Logic Điều hướng Đọc tiếp & Cập nhật Trạng thái (Resume Reading)**

* **Tình huống:** Người dùng bấm vào một thẻ truyện bất kỳ trong danh sách lịch sử.
* **Logic xử lý:**
    * App trích xuất id của cuốn sách được chọn và truyền id này sang **UI 3 (Reader Screen)**.
    * Trong background, BookManager cập nhật biến lastReadTime \= System.currentTimeMillis() cho cuốn sách đó để đẩy nó lên đầu danh sách lịch sử ở lần sau.
    * Chuyển hướng màn hình lập tức sang UI 3\. UI 3 sẽ đọc các trường lastChapterIndex và lastPosition để cuộn/nhảy tới đúng trang dở dang.

#### **3\. Logic Xóa Lịch sử (Single & Bulk Delete)**

* **Tình huống:** Người dùng muốn dọn dẹp lịch sử (Xóa từng mục hoặc Xóa toàn bộ).
* **Logic xử lý:**
    * **Xóa 1 mục:** Khi bấm nút thùng rác bên cạnh 1 cuốn truyện (hoặc vuốt sang trái), DatabaseControl gọi lệnh UPDATE thiết lập lastReadTime \= 0 cho cuốn sách đó trong DB.  
      *(Lưu ý: Không xóa hoàn toàn cuốn sách khỏi bảng books, chỉ reset mốc thời gian đọc về 0 để nó biến mất khỏi UI 7 nhưng vẫn còn trong Thư viện UI 1).*
    * **Xóa tất cả (Clear All):** Khi bấm nút "Xóa lịch sử":
        1. Hộp thoại xác nhận (AlertDialog) hiện lên: *"Bạn có chắc chắn muốn xóa toàn bộ lịch sử đọc?"*.
        2. Nếu chọn "Xóa": Chạy lệnh SQL UPDATE chuyển toàn bộ lastReadTime \= 0.
        3. UI 7 tự động chuyển sang trạng thái Giao diện trống (Empty State).

#### **4\. Logic Xử lý Ngoại lệ (Edge Cases \- Chặn Lỗi)**

* **Trường hợp Lịch sử trống:** Người dùng mới tải app hoặc vừa xóa sạch lịch sử.
    * *Cách xử lý:* Nếu danh sách trả về bằng null hoặc độ dài bằng 0 (list.isEmpty()), ẩn toàn bộ danh sách và nút "Xóa tất cả", hiển thị một hình ảnh minh họa (Illustration) kèm dòng chữ: *"Chưa có lịch sử đọc truyện"*.
* **Trường hợp File gốc bị xóa khỏi điện thoại:** Cuốn truyện vẫn có trong lịch sử nhưng người dùng đã dùng app quản lý file bên ngoài để xóa file .txt/.epub thực tế trên máy.
    * *Cách xử lý:* Khi bấm vào thẻ truyện, BookManager kiểm tra File(filePath).exists(). Nếu file không còn tồn tại, hiển thị thông báo Toast: *"File truyện đã bị xóa hoặc di chuyển khỏi bộ nhớ máy\!"* và hỏi người dùng có muốn dọn dẹp dòng này khỏi Thư viện luôn hay không.

### **🛠 LOGIC CHI TIẾT CHO UI 8: TÌM KIẾM TỨC THÌ (SEARCH SCREEN)**

Màn hình này xử lý tra cứu theo thời gian thực (Realtime Search) kết hợp với các bộ lọc tiêu chí đa dạng.

#### **1\. Logic Lọc & Tìm kiếm Tức thì (Realtime Search & Filter)**

* **Tình huống:** Người dùng gõ ký tự vào thanh tìm kiếm hoặc bấm chọn các thẻ bộ lọc (Filter Chips).
* **Logic xử lý:**
    * **Chống lag khi gõ (Debounce Technique):** Khi người dùng gõ liên tục (ví dụ: gõ chữ "T-i-ê-n H-i-ệ-p"), app không thực hiện truy vấn DB ngay sau mỗi ký tự gõ vào (tránh quá tải UI). Lập trình viên thiết lập một khoảng hoãn (Debounce delay $\\sim 300\\text{ms}$). Khi người dùng dừng gõ đủ $300\\text{ms}$, truy vấn mới chính thức được gửi đi.
    * **Thực thi Query động trong SQLite:** Truy vấn kết hợp cả Từ khóa \+ Thể loại \+ Trạng thái:
    * SQL  
      SELECT \* FROM books   
      WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')  
      AND (:tag \= '' OR tags LIKE '%' || :tag || '%')  
      AND (:status \= '' OR status \= :status)
    * **Chuẩn hóa chuỗi (Unaccent Match):** Xử lý hỗ trợ tìm kiếm tiếng Việt có dấu và không dấu (ví dụ: gõ "tien hiep" vẫn tìm ra truyện "Tiên Hiệp").

#### **2\. Logic Tương tác Thẻ Bộ lọc (Filter Chips Interaction)**

* **Tình huống:** Người dùng chạm vào các thẻ như \[Tiên Hiệp\], \[Đã hoàn thành\], \[.EPUB\].
* **Logic xử lý:**
    * Thẻ được chọn sẽ đổi trạng thái thị giác (Highlight màu nền).
    * Biến trạng thái selectedTag hoặc selectedStatus trong DatabaseControl được cập nhật.
    * Luồng tìm kiếm kích hoạt lại tự động để trả về kết quả khớp với cả từ khóa tìm kiếm (nếu có) \+ các thẻ đang chọn.
    * Bấm lần 2 vào thẻ đó sẽ hủy chọn (Toggle Off) và khôi phục trạng thái tìm kiếm ban đầu.

#### **3\. Logic Xử lý Nút Xóa nhanh (Clear Action) & Lịch sử Từ khóa**

* **Tình huống:** Người dùng muốn đổi từ khóa khác hoặc xóa trắng tìm kiếm.
* **Logic xử lý:**
    * **Nút "X" trên thanh Search Bar:** Chỉ xuất hiện khi có ít nhất 1 ký tự trong ô nhập. Bấm vào nút này sẽ làm rỗng chuỗi văn bản (queryText \= ""), bàn phím vẫn giữ nguyên và danh sách lập tức trả về trạng thái hiển thị mặc định.
    * **Ẩn Bàn phím mềm (Hide Keyboard):** Khi người dùng vuốt danh sách kết quả bên dưới hoặc bấm nút "Search/Enter" trên bàn phím, app phát lệnh ẩn bàn phím mềm để nhường không gian hiển thị cho danh sách kết quả.

#### **4\. Logic Xử lý Ngoại lệ (Edge Cases \- Chặn Lỗi)**

* **Trường hợp Không tìm thấy kết quả (No Search Results):** Từ khóa nhập vào không trùng khớp với bất kỳ cuốn sách nào trong DB.
    * *Cách xử lý:* Hiển thị Giao diện trống (Empty State) với biểu tượng Kính lúp kèm thông báo: *"Không tìm thấy truyện nào phù hợp với từ khóa '\[Từ\_Khóa\]'"* kèm nút "Xóa bộ lọc" để người dùng dễ dàng thử lại.
* **Trường hợp Từ khóa chứa Ký tự Đặc biệt:** Người dùng vô tình hoặc cố ý nhập các ký tự đặc biệt của SQL/Regex (như %, \_, ', ", \\).
    * *Cách xử lý:* Trước khi đưa chuỗi query vào câu lệnh SQL, logic phải thực hiện Sanitize (làm sạch/escape) các ký tự này để tránh lỗi crash chương trình hoặc lỗi truy vấn SQL Syntax Error.

