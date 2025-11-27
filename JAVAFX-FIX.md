# Fix JavaFX Runtime Components Error

## Lỗi: "JavaFX runtime components are missing"

### ✅ Giải pháp 1: Chạy bằng Maven (Khuyến nghị - Dễ nhất)

Đây là cách **ĐƠN GIẢN NHẤT** và đảm bảo hoạt động:

```bash
cd ChatClient
mvn clean javafx:run
```

Maven sẽ tự động handle tất cả JavaFX dependencies!

---

### ✅ Giải pháp 2: Configure IntelliJ IDEA đúng cách

#### Bước 1: Add VM Options

1. **Run → Edit Configurations...**
2. Chọn hoặc tạo configuration cho `ChatClientApp`
3. Trong phần **VM options**, thêm:

```
--module-path "/path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.media
```

#### Bước 2: Tìm JavaFX SDK path

**Trên Linux/Mac:**
```bash
# Maven sẽ download JavaFX vào local repository
# Thường ở đây:
ls ~/.m2/repository/org/openjfx/javafx-controls/21.0.1/

# Hoặc install JavaFX SDK:
# Download từ: https://gluonhq.com/products/javafx/
# Extract và dùng path đó
```

**Trên Windows:**
```
C:\Users\YourUser\.m2\repository\org\openjfx\javafx-controls\21.0.1\
```

#### Bước 3: VM Options đầy đủ

Nếu bạn tìm thấy JavaFX SDK, VM options sẽ như sau:

**Linux/Mac:**
```
--module-path /home/user/.m2/repository/org/openjfx:$PATH_TO_JAVAFX_SDK/lib --add-modules javafx.controls,javafx.fxml,javafx.media
```

**Windows:**
```
--module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.media
```

---

### ✅ Giải pháp 3: Sử dụng Maven Exec Plugin

Thêm configuration này vào `ChatClient/pom.xml`:

```xml
<build>
    <plugins>
        <!-- Existing javafx-maven-plugin -->
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.chatapp.client.ChatClientApp</mainClass>
            </configuration>
        </plugin>

        <!-- Add this exec plugin -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>com.chatapp.client.ChatClientApp</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Sau đó chạy:
```bash
mvn clean compile exec:java
```

---

### ✅ Giải pháp 4: Install OpenJFX (Cho Linux)

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjfx

# Fedora/RHEL
sudo dnf install java-openjfx

# Arch Linux
sudo pacman -S java-openjfx
```

---

### ✅ Giải pháp 5: Sử dụng JDK có built-in JavaFX

Download và cài đặt **Liberica JDK Full** (có JavaFX built-in):
- Link: https://bell-sw.com/pages/downloads/
- Chọn phiên bản **Full** (không phải Standard)
- Set làm JDK mặc định trong IntelliJ

---

## 🔧 Quick Fix - Chạy ngay (Không cần config)

### Option A: Maven Clean + Run
```bash
cd ChatClient
mvn clean compile javafx:run
```

### Option B: IntelliJ với Maven
1. Mở IntelliJ
2. Vào Maven panel (bên phải)
3. Expand: `ChatClient → Plugins → javafx`
4. Double-click: `javafx:run`

![Maven Panel](https://i.imgur.com/example.png)

---

## 🎯 Recommended Workflow

### Cho Development (IDE):

1. **Install Liberica JDK Full** hoặc configure VM options
2. Run từ IntelliJ với configuration đúng

### Cho Testing nhanh:

```bash
# Terminal 1 - Server
cd ChatServer
mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"

# Terminal 2 - Client 1
cd ChatClient
mvn javafx:run

# Terminal 3 - Client 2
cd ChatClient
mvn javafx:run
```

---

## 🐛 Troubleshooting

### Lỗi: "Graphics Device initialization failed"

**Linux users cần X11:**
```bash
export DISPLAY=:0
mvn javafx:run
```

### Lỗi: Module javafx.controls not found

```bash
# Verify JavaFX dependencies
mvn dependency:tree | grep javafx

# Re-download dependencies
mvn clean install -U
```

### Lỗi: Class not found

```bash
# Clean và rebuild
mvn clean compile
mvn javafx:run
```

---

## ✅ Verify Setup thành công

Khi chạy đúng, bạn sẽ thấy:

```
[INFO] --- javafx-maven-plugin:0.0.8:run (default-cli) @ ChatClient ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 7 source files to /path/to/ChatClient/target/classes
```

Và Login window sẽ hiển thị!

---

## 💡 Pro Tips

### 1. Create Run Script

**run-client.sh** (Linux/Mac):
```bash
#!/bin/bash
cd ChatClient
mvn javafx:run
```

```bash
chmod +x run-client.sh
./run-client.sh
```

**run-client.bat** (Windows):
```batch
@echo off
cd ChatClient
mvn javafx:run
```

### 2. IntelliJ Run Configuration

Tạo Run Configuration với:
- **Name**: ChatClient
- **Working directory**: `$PROJECT_DIR$/ChatClient`
- **Command**: `javafx:run`
- **Run Maven Goal**: Checked

### 3. Build Executable JAR

Nếu muốn tạo JAR có thể chạy độc lập:

```bash
cd ChatClient
mvn clean package

# Run with:
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -jar target/ChatClient-1.0-SNAPSHOT.jar
```

---

## 🎓 Hiểu vấn đề

JavaFX không còn đi kèm với JDK từ Java 11+. Có 3 cách load JavaFX:

1. ✅ **Maven plugin** (javafx:run) - Tự động handle
2. ⚠️ **VM options** (--module-path) - Cần config thủ công
3. ✅ **JDK Full** (Liberica) - JavaFX built-in

**Best practice**: Sử dụng Maven plugin cho development!

---

## Need Help?

Nếu vẫn gặp lỗi, hãy gửi:
1. Output của: `java -version`
2. Output của: `mvn -version`
3. Full error message
4. Operating system

---

**Happy Coding! 🚀**

Chạy command này là đơn giản nhất:
```bash
cd ChatClient && mvn javafx:run
```
