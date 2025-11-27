# Chat Application - Ứng dụng Chat Desktop với JavaFX

Ứng dụng chat desktop đa tính năng, hoạt động theo mô hình Client-Server, sử dụng JavaFX cho giao diện người dùng và MySQL làm cơ sở dữ liệu.

## 📋 Mục lục

- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt và cấu hình](#-cài-đặt-và-cấu-hình)
- [Chạy ứng dụng](#-chạy-ứng-dụng)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)

## ✨ Tính năng

### 1. Quản lý Người dùng & Xác thực
- ✅ Đăng ký tài khoản mới
- ✅ Đăng nhập/Đăng xuất
- ✅ Cập nhật profile (Full Name, Status Message)
- ✅ Thay đổi trạng thái (Online, Offline, Away, Busy)

### 2. Quản lý Bạn bè
- ✅ Tìm kiếm người dùng theo Username/Email
- ✅ Gửi/Nhận lời mời kết bạn
- ✅ Chấp nhận/Từ chối lời mời kết bạn
- ✅ Danh sách bạn bè với trạng thái real-time
- ✅ Xem profile người dùng

### 3. Tin nhắn & Chat
- ✅ Chat 1:1 (Private Chat)
- ✅ Chat nhóm (Group Chat)
- ✅ Lịch sử tin nhắn
- ✅ Tin nhắn văn bản (TEXT)
- ✅ Gửi hình ảnh (IMAGE)
- ✅ Gửi file đính kèm (FILE)
- ✅ Emoji support

### 4. Quản lý Nhóm
- ✅ Tạo nhóm chat mới
- ✅ Xem danh sách nhóm
- ✅ Tham gia/Rời nhóm
- ✅ Thêm thành viên vào nhóm

### 5. Cuộc gọi (Voice/Video - Signaling)
- ✅ Khởi tạo cuộc gọi thoại
- ✅ Khởi tạo cuộc gọi video
- ✅ Nhận/Chấp nhận/Từ chối cuộc gọi
- ✅ Kết thúc cuộc gọi
- ⚠️ *Lưu ý: Chỉ có signaling cơ bản, chưa có media streaming thực tế*

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ**: Java 17+
- **GUI Framework**: JavaFX 21
- **Database**: MySQL 8.0+
- **Build Tool**: Maven
- **Networking**: Socket TCP
- **JSON Processing**: Gson
- **Password Hashing**: BCrypt
- **IDE**: IntelliJ IDEA (khuyến nghị)

## 🏗 Kiến trúc hệ thống

Dự án được chia thành 2 module Maven độc lập:

```
chat-app-v2/
├── ChatServer/          # Server application (bao gồm models, protocol)
└── ChatClient/          # Client application (JavaFX, bao gồm models, protocol)
```

**Lưu ý**: Mỗi module chứa các class chung (enums, models, protocol) riêng biệt để đảm bảo tính độc lập.

### Mô hình Client-Server

```
┌─────────────┐                ┌─────────────┐
│   Client 1  │◄──────────────►│             │
└─────────────┘    Socket TCP  │             │
                                │   Server    │
┌─────────────┐                │             │
│   Client 2  │◄──────────────►│  (Port 8888)│
└─────────────┘                │             │
                                │      ↕      │
┌─────────────┐                │   MySQL DB  │
│   Client N  │◄──────────────►│             │
└─────────────┘                └─────────────┘
```

## 📦 Yêu cầu hệ thống

### Phần mềm cần thiết:

1. **Java Development Kit (JDK) 17 hoặc cao hơn**
   ```bash
   java -version
   ```

2. **Apache Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL Server 8.0+**
   ```bash
   mysql --version
   ```

4. **IntelliJ IDEA** (khuyến nghị) hoặc IDE hỗ trợ Maven và JavaFX

## 🚀 Cài đặt và cấu hình

### Bước 1: Clone hoặc tải dự án

```bash
cd chat-app-v2
```

### Bước 2: Cấu hình MySQL Database

1. Khởi động MySQL server

2. Tạo database và import schema:

```bash
mysql -u root -p < database/schema.sql
```

Hoặc thực hiện thủ công:

```sql
CREATE DATABASE chat_app_db;
USE chat_app_db;
-- Sau đó copy và chạy nội dung của file database/schema.sql
```

3. Cấu hình database connection trong `ChatServer/src/main/resources/server.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/chat_app_db?useSSL=false&serverTimezone=UTC
db.username=root
db.password=your_password_here
```

### Bước 3: Build dự án

```bash
# Build tất cả modules
mvn clean install
```

## 🎮 Chạy ứng dụng

### Chạy Server

**Cách 1: Sử dụng Maven**
```bash
cd ChatServer
mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
```

**Cách 2: Sử dụng JAR file**
```bash
cd ChatServer
mvn clean package
java -jar target/ChatServer-1.0-SNAPSHOT.jar
```

**Cách 3: Từ IntelliJ IDEA**
- Mở class `ChatServer.java`
- Click chuột phải → Run 'ChatServer.main()'

Server sẽ khởi động trên port **8888** (mặc định)

### Chạy Client

**Cách 1: Sử dụng Maven**
```bash
cd ChatClient
mvn javafx:run
```

**Cách 2: Từ IntelliJ IDEA**
- Mở class `ChatClientApp.java`
- Click chuột phải → Run 'ChatClientApp.main()'

**Lưu ý**: Bạn có thể chạy nhiều client cùng lúc bằng cách chạy lại lệnh trên trong terminal khác hoặc sử dụng tính năng "Allow multiple instances" trong IntelliJ.

## 📁 Cấu trúc dự án

```
chat-app-v2/
│
├── pom.xml                          # Parent POM
│
├── database/
│   └── schema.sql                   # Database schema
│
├── ChatServer/                      # Module Server (độc lập)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/chatapp/server/
│       │   ├── ChatServer.java      # Main server class
│       │   ├── enums/               # Enums (UserStatus, MessageType, etc.)
│       │   ├── model/               # Models (User, Message, Group, etc.)
│       │   ├── protocol/            # Packet protocol
│       │   ├── handler/             # ClientHandler
│       │   ├── service/             # Business logic services
│       │   ├── dao/                 # Database access objects
│       │   └── util/                # Utilities (DatabaseManager, Config)
│       └── resources/
│           └── server.properties    # Server configuration
│
└── ChatClient/                      # Module Client (độc lập)
    ├── pom.xml
    └── src/main/
        ├── java/com/chatapp/client/
        │   ├── ChatClientApp.java   # Main JavaFX application
        │   ├── enums/               # Enums (UserStatus, MessageType, etc.)
        │   ├── model/               # Models (User, Message, Group, etc.)
        │   ├── protocol/            # Packet protocol
        │   ├── controller/          # FXML Controllers
        │   └── service/             # Client services
        └── resources/
            ├── fxml/                # FXML files
            ├── css/                 # Stylesheets
            └── client.properties    # Client configuration
```

**Lưu ý**: Server và Client là 2 module hoàn toàn độc lập, mỗi module chứa bản sao riêng của các class chung (enums, model, protocol).

## 📖 Hướng dẫn sử dụng

### 1. Đăng ký tài khoản

1. Khởi động Client
2. Trên màn hình Login, click "Register"
3. Nhập thông tin:
   - Full Name
   - Username (duy nhất)
   - Email (duy nhất)
   - Password (tối thiểu 6 ký tự)
4. Click "Register"

### 2. Đăng nhập

1. Nhập Username và Password
2. Cấu hình Server (nếu cần):
   - Host: `localhost` (mặc định)
   - Port: `8888` (mặc định)
3. Click "Login"

### 3. Thêm bạn bè

1. Click nút "Search" ở sidebar
2. Nhập username hoặc email để tìm kiếm
3. Chọn người dùng và gửi lời mời kết bạn
4. Người nhận sẽ thấy lời mời trong tab "Requests"

### 4. Chat 1:1

1. Chọn bạn bè từ danh sách trong tab "Friends"
2. Gõ tin nhắn trong ô input
3. Nhấn Enter hoặc click "Send"
4. Sử dụng nút 📎 để gửi file/hình ảnh
5. Sử dụng nút 😊 để chọn emoji

### 5. Tạo nhóm chat

1. Vào tab "Groups"
2. Click "+ Create Group"
3. Nhập tên nhóm
4. Chọn thành viên từ danh sách bạn bè
5. Click "Create"

### 6. Cuộc gọi

1. Mở chat 1:1 với bạn bè
2. Click nút "📞 Call" cho cuộc gọi thoại
3. Click nút "📹 Video" cho cuộc gọi video
4. Người nhận có thể chấp nhận hoặc từ chối

### 7. Thay đổi trạng thái

1. Click menu "Status" ở góc trên
2. Chọn:
   - **Online**: Đang trực tuyến
   - **Away**: Vắng mặt
   - **Busy**: Bận

## ⚙️ Cấu hình nâng cao

### Server Configuration (server.properties)

```properties
# Port server
server.port=8888

# Thread pool size
server.thread.pool.size=50

# Database
db.url=jdbc:mysql://localhost:3306/chat_app_db
db.username=root
db.password=
db.pool.size=20

# File transfer
file.upload.dir=uploads/
file.chunk.size=65536
```

### Client Configuration (client.properties)

```properties
# Server connection
server.host=localhost
server.port=8888

# Reconnection
client.reconnect.attempts=5
client.reconnect.delay=3000
```

## 🔧 Troubleshooting

### Lỗi kết nối database

**Lỗi**: `Connection refused` hoặc `Access denied`

**Giải pháp**:
1. Kiểm tra MySQL server đã khởi động
2. Xác nhận username/password trong `server.properties`
3. Kiểm tra database `chat_app_db` đã được tạo

### Lỗi không kết nối được Server

**Lỗi**: Client không kết nối được đến Server

**Giải pháp**:
1. Đảm bảo Server đã khởi động và running
2. Kiểm tra port 8888 không bị firewall chặn
3. Xác nhận host và port trong Client settings

### Lỗi JavaFX không load được

**Lỗi**: `Error initializing QuantumRenderer`

**Giải pháp**:
1. Đảm bảo JDK có JavaFX hoặc sử dụng OpenJFX
2. Chạy bằng Maven: `mvn javafx:run`

## 📝 Ghi chú

- **Mật khẩu**: Được hash bằng BCrypt trước khi lưu vào database
- **Giao thức**: Sử dụng JSON qua Socket TCP
- **Threading**: Server sử dụng ThreadPool để xử lý multiple clients
- **UI Thread**: Client sử dụng `Platform.runLater()` để update UI safely

## 🔐 Bảo mật

- Mật khẩu được hash với BCrypt (cost factor: 12)
- Không lưu plain-text password
- Session-based authentication
- Input validation ở cả client và server

## 🚧 Giới hạn & Cải tiến tương lai

### Giới hạn hiện tại:
- Cuộc gọi voice/video chỉ có signaling, chưa có media streaming
- File transfer có giới hạn kích thước
- Không có encryption cho tin nhắn
- Chưa có message read receipts

### Cải tiến tương lai:
- [ ] Implement WebRTC cho voice/video call
- [ ] End-to-end encryption
- [ ] Message read/delivered status
- [ ] Typing indicators
- [ ] Push notifications
- [ ] Message search
- [ ] File preview
- [ ] User avatars
- [ ] Dark mode

## 👨‍💻 Tác giả

Dự án được phát triển cho môn học Lập trình mạng - Desktop Chat Application

## 📄 License

Dự án này được phát triển cho mục đích học tập.

---

**Chúc bạn sử dụng ứng dụng vui vẻ! 🎉**

Nếu gặp vấn đề, vui lòng tạo issue hoặc liên hệ với giảng viên hướng dẫn.
