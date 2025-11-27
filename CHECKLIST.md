# Checklist - Setup & Test Chat Application

## ✅ Pre-requisites

- [ ] Java 17+ installed
  ```bash
  java -version
  # Should show: java version "17" or higher
  ```

- [ ] Maven 3.6+ installed
  ```bash
  mvn -version
  # Should show: Apache Maven 3.6.x or higher
  ```

- [ ] MySQL 8.0+ installed and running
  ```bash
  mysql --version
  # Should show: mysql Ver 8.0.x
  ```

## ✅ Database Setup

- [ ] MySQL server is running
  ```bash
  # Linux
  sudo systemctl status mysql

  # Mac
  brew services list | grep mysql

  # Windows
  # Check Services app
  ```

- [ ] Create database
  ```bash
  mysql -u root -p
  ```
  ```sql
  CREATE DATABASE chat_app_db;
  USE chat_app_db;
  SOURCE /home/user/chat-app-v2/database/schema.sql;
  EXIT;
  ```

- [ ] Verify tables created
  ```sql
  USE chat_app_db;
  SHOW TABLES;
  # Should show: users, friends, friend_requests, groups, group_members, messages, call_logs
  ```

## ✅ Configure Server

- [ ] Edit `ChatServer/src/main/resources/server.properties`
  - [ ] Set correct MySQL username (default: root)
  - [ ] Set correct MySQL password
  - [ ] Verify database URL
  - [ ] Check port 8888 is available

## ✅ Build Project

- [ ] Navigate to project root
  ```bash
  cd /home/user/chat-app-v2
  ```

- [ ] Clean and build all modules
  ```bash
  mvn clean install
  ```

- [ ] Verify build success
  ```
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```

## ✅ Start Server

- [ ] Open Terminal 1
  ```bash
  cd ChatServer
  mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
  ```

- [ ] Verify server started successfully
  ```
  ✓ Database connection successful
  ✓ Thread pool initialized with 50 threads
  ✓ Chat Server started on port 8888
  ✓ Waiting for clients...
  ```

## ✅ Start Client(s)

- [ ] Open Terminal 2 (Client 1)
  ```bash
  cd ChatClient
  mvn javafx:run
  ```

- [ ] Verify Login window appears
  - [ ] Window title: "Login - Chat Application"
  - [ ] Username field visible
  - [ ] Password field visible
  - [ ] Server settings visible (localhost:8888)

- [ ] Open Terminal 3 (Client 2) - Optional
  ```bash
  cd ChatClient
  mvn javafx:run
  ```

## ✅ Test Features

### Registration & Login

- [ ] **Client 1 - Register User "Alice"**
  - [ ] Click "Register" link
  - [ ] Fill in:
    - Full Name: `Alice Johnson`
    - Username: `alice`
    - Email: `alice@test.com`
    - Password: `password123`
    - Confirm Password: `password123`
  - [ ] Click "Register" button
  - [ ] Success message appears
  - [ ] Redirected to Login screen

- [ ] **Client 1 - Login as Alice**
  - [ ] Username: `alice`
  - [ ] Password: `password123`
  - [ ] Click "Login"
  - [ ] Main window appears
  - [ ] Title shows: "Chat Application - alice"

- [ ] **Client 2 - Register User "Bob"**
  - [ ] Full Name: `Bob Smith`
  - [ ] Username: `bob`
  - [ ] Email: `bob@test.com`
  - [ ] Password: `password123`

- [ ] **Client 2 - Login as Bob**
  - [ ] Login successful
  - [ ] Main window appears

### Friend Management

- [ ] **Bob searches for Alice**
  - [ ] Enter "alice" in search field
  - [ ] Click "Search"
  - [ ] Alice appears in results
  - [ ] Click "Add Friend"
  - [ ] Friend request sent confirmation

- [ ] **Alice receives friend request**
  - [ ] Go to "Requests" tab
  - [ ] Bob's friend request appears
  - [ ] Shows: Bob Smith (@bob)
  - [ ] "Accept" and "Reject" buttons visible

- [ ] **Alice accepts friend request**
  - [ ] Click "Accept" button
  - [ ] Request disappears from list
  - [ ] Go to "Friends" tab
  - [ ] Bob appears in friend list

- [ ] **Bob sees Alice as friend**
  - [ ] Go to "Friends" tab
  - [ ] Alice appears in friend list
  - [ ] Status shows "ONLINE" with green dot

### Private Chat (1:1)

- [ ] **Alice opens chat with Bob**
  - [ ] Click on Bob in Friends list
  - [ ] Chat window opens on right side
  - [ ] Header shows: "Bob Smith (@bob)"
  - [ ] Message input visible
  - [ ] Call buttons visible

- [ ] **Alice sends message to Bob**
  - [ ] Type: "Hello Bob!"
  - [ ] Press Enter or click Send
  - [ ] Message appears in blue bubble (right side)
  - [ ] Timestamp shows current time

- [ ] **Bob receives and replies**
  - [ ] Bob's window: New message appears
  - [ ] Message shows in gray bubble (left side)
  - [ ] Shows: alice: "Hello Bob!"
  - [ ] Bob replies: "Hi Alice!"
  - [ ] Alice receives reply instantly

### Group Chat

- [ ] **Alice creates group**
  - [ ] Go to "Groups" tab
  - [ ] Click "+ Create Group"
  - [ ] Enter group name: "Test Group"
  - [ ] Select Bob as member
  - [ ] Click "Create"
  - [ ] Group appears in list

- [ ] **Bob receives group notification**
  - [ ] Bob sees "Test Group" in Groups tab
  - [ ] Shows: 2 members

- [ ] **Group chat messaging**
  - [ ] Alice clicks on "Test Group"
  - [ ] Alice sends: "Welcome to the group!"
  - [ ] Bob sees message in group
  - [ ] Bob replies: "Thanks!"
  - [ ] Both see all messages

### Status Management

- [ ] **Alice changes status**
  - [ ] Click "Status" menu button
  - [ ] Select "Away"
  - [ ] Status updated

- [ ] **Bob sees Alice's status change**
  - [ ] Alice's status in friend list changes to "AWAY"
  - [ ] Status indicator color changes (orange)

### Call Features (Signaling)

- [ ] **Alice initiates call to Bob**
  - [ ] Open private chat with Bob
  - [ ] Click "📞 Call" button
  - [ ] Call initiated message

- [ ] **Bob receives call notification**
  - [ ] Popup/notification appears
  - [ ] Shows: "Incoming call from alice"
  - [ ] "Accept" and "Reject" buttons

- [ ] **Bob accepts call**
  - [ ] Click "Accept"
  - [ ] Call connected message
  - [ ] Both users see call active

- [ ] **End call**
  - [ ] Either user clicks "End Call"
  - [ ] Call ends for both parties

### File Transfer

- [ ] **Alice sends file to Bob**
  - [ ] In chat with Bob
  - [ ] Click 📎 (attach file) button
  - [ ] Select a test image (< 5MB)
  - [ ] File uploads
  - [ ] Bob receives file notification

## ✅ Server Logs Verification

Check Terminal 1 (Server) for these logs:

- [ ] New user registered: alice
- [ ] New user registered: bob
- [ ] User logged in: alice
- [ ] User logged in: bob
- [ ] Friend request sent: bob -> alice
- [ ] Friend request accepted
- [ ] Private message sent: alice -> bob
- [ ] Group created: Test Group
- [ ] Call initiated: alice -> bob

## ✅ Database Verification

```sql
USE chat_app_db;

-- Check users
SELECT id, username, full_name, status FROM users;
-- Should show: alice and bob

-- Check friends
SELECT * FROM friends;
-- Should show: bidirectional friendship

-- Check messages
SELECT sender_id, receiver_id, content, message_type FROM messages;
-- Should show: chat messages

-- Check groups
SELECT * FROM groups;
-- Should show: Test Group

-- Check group members
SELECT * FROM group_members;
-- Should show: alice and bob in group
```

## ✅ Cleanup & Logout

- [ ] **Both users logout**
  - [ ] Click "Logout" button
  - [ ] Redirected to Login screen
  - [ ] Server logs show: User logged out

- [ ] **Stop server**
  - [ ] Press Ctrl+C in Terminal 1
  - [ ] Server shuts down gracefully
  - [ ] Database connections closed

## 🎉 Success Criteria

All features working if:

✅ Registration and login successful
✅ Friend requests working
✅ Private chat messages delivered instantly
✅ Group chat functional
✅ Status changes reflected real-time
✅ Call signaling works
✅ File transfers complete
✅ Server handles multiple clients
✅ Database stores all data correctly
✅ No crashes or errors

## 📝 Notes

- **Port conflicts**: If port 8888 is busy, change in both server.properties and client.properties
- **Database connection**: Verify MySQL credentials are correct
- **JavaFX issues**: Use `mvn javafx:run` instead of running JAR directly
- **Multiple clients**: Can run as many clients as needed for testing

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Server won't start | Check MySQL is running and credentials are correct |
| Client won't start | Use `mvn javafx:run` instead of `java -jar` |
| Connection refused | Verify server is running and port is correct |
| Friend list empty | Check database for friends table entries |
| Messages not delivering | Verify both users are online/connected |

---

**Testing completed successfully?** ✅

Your Chat Application is ready for demo/submission! 🎓
