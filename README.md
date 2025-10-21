# 🚀 LTM - Learning Task Management System

A comprehensive push notification system using Firebase Cloud Messaging (FCM) with Android client, Java server, and Swing administration client.

## 📋 Overview

LTM is a multi-platform system that enables sending push notifications to Android devices through Firebase Cloud Messaging. The system consists of:

- **Android App**: Receives and displays push notifications
- **Java Server**: REST API for FCM token management and notification sending
- **Swing Client**: Administration interface for testing and management
- **MySQL Database**: Stores FCM tokens and device information

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │  Java Server    │    │  Swing Client   │
│                 │    │                 │    │                 │
│ • FCM Token     │◄──►│ • REST API      │◄──►│ • Admin UI      │
│ • Notifications │    │ • FCM v1 API    │    │ • Testing       │
│ • Token Refresh │    │ • Database      │    │ • Debug Tools   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│  Firebase FCM   │    │  MySQL Database │
│                 │    │                 │
│ • Push Service  │    │ • Token Storage │
│ • Authentication│    │ • Device Info   │
└─────────────────┘    └─────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Java 17** - Server runtime
- **Jakarta Servlet** - Web framework
- **Jetty** - Application server
- **MySQL 8+** - Database
- **Firebase Admin SDK** - FCM integration

### Frontend
- **Android SDK** - Mobile application
- **Java Swing** - Desktop client
- **Firebase Cloud Messaging** - Push notifications

## 📁 Project Structure

```
LTM/
├── android-sample/          # Android application
│   ├── app/
│   │   ├── src/main/java/com/example/fcmtest/
│   │   │   ├── MainActivity.java
│   │   │   └── MyFirebaseMessagingService.java
│   │   ├── google-services.json
│   │   └── build.gradle
│   └── build.gradle
├── server/                  # Java server
│   ├── src/main/java/com/example/server/
│   │   ├── ServerServlet.java
│   │   ├── FCMSender.java
│   │   ├── DatabaseHelper.java
│   │   └── ConfigLoader.java
│   ├── src/main/resources/config.properties
│   └── ltmck-90f36-firebase-adminsdk-fbsvc-4424fdba48.json
├── client/                  # Swing client
│   ├── src/main/java/com/example/client/
│   │   ├── ClientApp.java
│   │   └── frmClient.java
│   └── pom.xml
├── db/                      # Database schema
│   └── schema.sql
└── pom.xml                  # Parent POM
```

## 🚀 Quick Start

### Prerequisites
- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8+**
- **Android Studio** (for Android app)
- **Firebase Project** with FCM enabled

### 1. Database Setup

```sql
-- Create database and table
CREATE DATABASE IF NOT EXISTS ltm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ltm;

CREATE TABLE IF NOT EXISTS device (
  id INT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(255) NOT NULL UNIQUE,
  label VARCHAR(100) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. Server Configuration

Edit `server/src/main/resources/config.properties`:

```properties
# Database Configuration
db.url=jdbc:mysql://localhost:3306/ltm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_password

# FCM Configuration
fcm.projectId=your-firebase-project-id
fcm.serviceAccountFile=path/to/your-service-account.json
```

### 3. Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Cloud Messaging API
3. Create a service account with Firebase Admin role
4. Download the service account JSON file
5. Update `config.properties` with the correct path

### 4. Run the System

#### Start Server
```bash
cd server
mvn jetty:run
```
Server will be available at `http://localhost:8080`

#### Start Swing Client
```bash
cd client
mvn exec:java -Dexec.mainClass="com.example.client.ClientApp"
```

#### Build Android App
1. Open `android-sample` in Android Studio
2. Build and install on device/emulator
3. Grant notification permissions when prompted

## 🔧 API Endpoints

### Server Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| POST | `/register` | Register FCM token | `token`, `label` |
| POST | `/send` | Send notification | `title`, `body` |
| POST | `/test-token` | Validate token format | `token` |
| POST | `/debug-token` | Test FCM API | `token` |
| POST | `/test-service-account` | Test service account | - |

### Example Usage

#### Register Token
```bash
curl -X POST http://localhost:8080/register \
  -d "token=YOUR_FCM_TOKEN&label=MyDevice"
```

#### Send Notification
```bash
curl -X POST http://localhost:8080/send \
  -d "title=Hello&body=Test notification"
```

## 📱 Android App Features

### MainActivity
- **Token Management**: Get, display, and refresh FCM tokens
- **Permission Handling**: Request notification permissions (Android 13+)
- **UI Components**: Token display, refresh button, copy functionality

### MyFirebaseMessagingService
- **Message Handling**: Process incoming FCM messages
- **Notification Display**: Create and show notifications
- **Channel Management**: Handle notification channels (Android 8+)

## 🖥️ Swing Client Features

### Administration Interface
- **Token Testing**: Validate FCM tokens
- **Notification Sending**: Send test notifications
- **Service Account Testing**: Verify Firebase authentication
- **Debug Tools**: Comprehensive testing and debugging

## 🔐 Security Features

- **Service Account Authentication**: Secure Firebase API access
- **Token Validation**: FCM token format verification
- **Database Security**: Prepared statements and input validation
- **Permission Management**: Android notification permissions

## 🐛 Troubleshooting

### Common Issues

#### 1. "INVALID_ARGUMENT" Error
- **Cause**: Invalid FCM token format
- **Solution**: Ensure token contains valid characters including colons `:`

#### 2. Service Account Authentication Failed
- **Cause**: Incorrect service account file path or permissions
- **Solution**: Verify file path and Firebase project permissions

#### 3. Database Connection Failed
- **Cause**: MySQL not running or incorrect credentials
- **Solution**: Start MySQL and verify connection settings

#### 4. Android Notifications Not Showing
- **Cause**: Missing notification permissions or channels
- **Solution**: Grant permissions and ensure proper channel setup

### Debug Tools

#### Server Logs
```bash
# Check server logs for detailed error information
tail -f server.log
```

#### Android Logcat
```bash
# Filter FCM logs
adb logcat | grep FCM
```

#### Database Queries
```sql
-- Check registered tokens
SELECT * FROM device;

-- Clear old tokens
DELETE FROM device WHERE token = 'old_token';
```

## 📊 Performance Considerations

- **Token Caching**: Tokens are cached in memory for performance
- **Database Optimization**: Indexed token column for fast lookups
- **FCM Rate Limits**: Respect Firebase rate limits
- **Error Handling**: Comprehensive error handling and logging

## 🚀 Deployment

### Production Setup

1. **Database**: Use production MySQL instance
2. **Server**: Deploy to application server (Tomcat, Jetty)
3. **Firebase**: Use production Firebase project
4. **Security**: Secure service account files
5. **Monitoring**: Implement logging and monitoring

### Environment Variables

```bash
export DB_URL="jdbc:mysql://prod-server:3306/ltm"
export DB_USER="ltm_user"
export DB_PASSWORD="secure_password"
export FCM_PROJECT_ID="your-prod-project"
export FCM_SERVICE_ACCOUNT="/path/to/prod-service-account.json"
```

## 📈 Future Enhancements

- [ ] **User Authentication**: Add user management system
- [ ] **Topic Messaging**: Support FCM topic subscriptions
- [ ] **Scheduled Notifications**: Add notification scheduling
- [ ] **Analytics**: Implement usage analytics
- [ ] **Web Dashboard**: Add web-based administration
- [ ] **Multi-tenant Support**: Support multiple projects

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the API documentation

---

**LTM - Learning Task Management System**  
*Empowering education through push notifications*
