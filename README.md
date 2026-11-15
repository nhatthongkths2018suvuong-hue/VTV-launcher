# VTV Launcher cho Android TV box (bản 2.0.0)

Đây là VTV Grid được nâng cấp thành **launcher (màn hình Home)** cho TV box.

## Cách hoạt động
- Bấm **Home** (hoặc bật box lên): app mở và tự phát **VTV1** (kênh tên "VTV1" trong channels.txt, không có thì kênh đầu tiên).
- Chuyển kênh như cũ: ▲▼ / CH+ CH- / phím số.
- **Back hoặc OK** khi đang xem: về màn hình chọn kênh.
- Ở màn hình chọn kênh, hàng trên cùng có 3 ô (bấm ▲ từ hàng kênh đầu tiên để lên, ◄► để di chuyển):
  - **Cài đặt**: mở Cài đặt của hệ thống Android.
  - **Ứng dụng**: danh sách tất cả ứng dụng đã cài; OK để mở. Giữ OK trên một ứng dụng để đặt làm ứng dụng nhanh.
  - **Ứng dụng nhanh**: lần đầu bấm OK để chọn ứng dụng; các lần sau OK để mở ngay.
    Muốn đổi: **giữ OK ~1 giây** trên ô này (hoặc bấm phím **Menu** trên remote, hoặc chạm giữ nếu dùng cảm ứng).
- Khi VTV Launcher là launcher mặc định, Back ở màn hình chọn kênh không thoát app (launcher không có chỗ để "thoát").
  Nếu chạy như app thường (không đặt làm mặc định) thì Back vẫn thoát như trước.

## Đặt làm launcher mặc định
Cài APK, bấm Home. Nếu box hỏi chọn màn hình chính, chọn **VTV Launcher** > "Luôn luôn".
Nếu box không hỏi (một số box khoá launcher), vào Cài đặt > Ứng dụng > Ứng dụng mặc định > Ứng dụng màn hình chính
(tên gọi tuỳ hãng box) rồi chọn VTV Launcher, hoặc dùng ADB:

    adb shell cmd package set-home-activity com.vtv.grid/.MainActivity

Muốn quay lại launcher cũ: Cài đặt > Ứng dụng > VTV Launcher > Xoá mặc định (Clear defaults), hoặc chọn lại launcher cũ ở mục trên.

---

# VTV Grid (bản gốc 1.4.0)

Chức năng giống bản LG webOS 1.4.0: lưới kênh 4 cột, OK để xem, mũi tên lên/xuống để chuyển kênh,
phím số để nhảy kênh, Back/OK để về danh sách, tự lấy tên kênh vtvgo, không có dòng trạng thái / mã phím.

## 1. Đặt file danh sách kênh (chỉ một vị trí duy nhất)

    /sdcard/Android/data/com.vtv.grid/files/channels.txt

(trong bộ nhớ trong của box, thường là /storage/emulated/0/Android/data/com.vtv.grid/files/channels.txt)

- Mở app một lần: app tự tạo thư mục này và chép file mẫu vào đó. Chỉ việc thay bằng file của bạn.
- Chép file bằng: ADB (`adb push channels.txt /sdcard/Android/data/com.vtv.grid/files/channels.txt`),
  hoặc trình quản lý file trên box (một số box Android 11+ chặn thư mục Android/data, khi đó dùng ADB).
- Nếu không đọc được file, màn hình sẽ hiện đúng đường dẫn app đang tìm.

## 2. Build ra APK

### Cách A: không cần cài gì (GitHub)
1. Tạo repo mới trên GitHub, tải toàn bộ thư mục này lên (giữ nguyên thư mục .github).
2. Vào tab Actions, chạy "Build APK" (hoặc tự chạy sau khi push).
3. Khi xong, tải artifact `VTVGrid-apk` về, giải nén ra `app-debug.apk`.

### Cách B: Android Studio
1. File > Open, chọn thư mục này, đợi Gradle đồng bộ (cần JDK 17, Android Studio tự có).
2. Build > Build Bundle(s) / APK(s) > Build APK(s).
3. File nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

### Cách C: dòng lệnh
    gradle assembleDebug        (Gradle 8.7, JDK 17, Android SDK 34)

## 3. Cài lên box
Chép `app-debug.apk` vào box rồi cài (bật "Cài đặt từ nguồn không xác định"), hoặc `adb install app-debug.apk`.
Nếu box có màn hình Android TV, app hiện trong danh sách ứng dụng (có banner); box thường thì nằm trong "Ứng dụng".

## Ghi chú
- Video phát bằng thẻ video của Android System WebView. Box có WebView quá cũ có thể không phát được HLS;
  cập nhật "Android System WebView" nếu gặp tình trạng này.
- Phím CH+/CH- trên remote (nếu có) hoạt động như mũi tên lên/xuống khi đang xem.
