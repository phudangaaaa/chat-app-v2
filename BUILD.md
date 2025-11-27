# Build & Run Guide - Chat Application

## Quick Start (5 phút)

### 1. Chuẩn bị Database

```bash
# Khởi động MySQL và tạo database
mysql -u root -p
```

```sql
CREATE DATABASE chat_app_db;
USE chat_app_db;
SOURCE database/schema.sql;
EXIT;
```

### 2. Cấu hình Server

Chỉnh sửa `ChatServer/src/main/resources/server.properties`:

```properties
db.password=your_mysql_password
```

### 3. Build Project

```bash
# Từ thư mục gốc chat-app-v2
mvn clean install
```

### 4. Chạy Server

**Terminal 1:**
```bash
cd ChatServer
mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
```

Bạn sẽ thấy:
```
Database connection successful
Thread pool initialized with 50 threads
Chat Server started on port 8888
Waiting for clients...
```

### 5. Chạy Client

**Terminal 2 (Client 1):**
```bash
cd ChatClient
mvn javafx:run
```

**Terminal 3 (Client 2):**
```bash
cd ChatClient
mvn javafx:run
```

### 6. Test Application

#### User 1:
1. Click "Register"
2. Tạo tài khoản: alice / alice@test.com / password123
3. Login với tài khoản vừa tạo

#### User 2:
1. Click "Register"
2. Tạo tài khoản: bob / bob@test.com / password123
3. Login với tài khoản vừa tạo
4. Search "alice" và gửi friend request
5. Alice chấp nhận friend request trong tab "Requests"
6. Bắt đầu chat!

## Build cho Production

### Build Server JAR

```bash
cd ChatServer
mvn clean package
java -jar target/ChatServer-1.0-SNAPSHOT.jar
```

### Build Client JAR

```bash
cd ChatClient
mvn clean package
# Run với:
java -jar target/ChatClient-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Maven không tìm thấy dependencies

```bash
mvn clean install -U
```

### Port 8888 đã được sử dụng

Đổi port trong `server.properties`:
```properties
server.port=9999
```

Và trong `client.properties`:
```properties
server.port=9999
```

### JavaFX error trên Linux

```bash
sudo apt-get install openjfx
```

### Database connection error

1. Kiểm tra MySQL đang chạy: `sudo systemctl status mysql`
2. Test connection: `mysql -u root -p`
3. Kiểm tra database đã tạo: `SHOW DATABASES;`

## Development với IntelliJ IDEA

### Import Project

1. File → Open → Chọn thư mục `chat-app-v2`
2. IntelliJ sẽ tự động nhận diện Maven project
3. Đợi Maven download dependencies

### Run Server

1. Mở `ChatServer/src/main/java/com/chatapp/server/ChatServer.java`
2. Click nút ▶️ màu xanh bên cạnh `public static void main`
3. Hoặc: Right-click → Run 'ChatServer.main()'

### Run Client

1. Mở `ChatClient/src/main/java/com/chatapp/client/ChatClientApp.java`
2. Click nút ▶️ màu xanh bên cạnh `public static void main`
3. Để chạy nhiều instances:
   - Run → Edit Configurations...
   - Chọn ChatClientApp
   - Check "Allow multiple instances"
   - Apply → OK

### Debug

- Đặt breakpoint bằng cách click vào gutter (bên trái số dòng)
- Click nút 🐛 (Debug) thay vì Run
- Sử dụng Debug console để xem variables

## Kiểm tra Build thành công

### Server logs nên hiển thị:

```
Database connection pool initialized with 20 connections
Database connection successful
Thread pool initialized with 50 threads
Chat Server started on port 8888
Waiting for clients...
New client connected: /127.0.0.1
User logged in: alice
Active clients: 1
```

### Client UI nên:

- Login screen hiển thị đúng
- Kết nối được đến server
- Đăng nhập thành công
- Hiển thị main chat interface
- Friend list, Groups, Requests tabs hoạt động

## Performance Tips

### Server

- Tăng thread pool size nếu có nhiều users:
  ```properties
  server.thread.pool.size=100
  ```

- Tăng database connection pool:
  ```properties
  db.pool.size=30
  ```

### Client

- Limit message history:
  ```java
  data.put("limit", 100); // Trong loadMessageHistory()
  ```

## Next Steps

Sau khi build và test thành công:

1. ✅ Đọc README.md để hiểu đầy đủ các tính năng
2. ✅ Test tất cả features: Chat, Groups, Friends, Calls
3. ✅ Kiểm tra database để xem data được lưu như thế nào
4. ✅ Thử nghiệm với nhiều clients đồng thời
5. ✅ Customize UI trong FXML files

## Support

Nếu gặp vấn đề:

1. Kiểm tra logs trong terminal
2. Xem lại BUILD.md và README.md
3. Đảm bảo Java 17+, Maven 3.6+, MySQL 8.0+ đã cài đặt đúng

---

**Happy Coding! 🚀**
