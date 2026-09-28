package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Opens, copies and migrates the app's local SQLite database ({@code SmartClassroom.db}).
 *
 * <p>The database ships pre-populated in {@code assets/} and is copied to internal storage on
 * first launch by {@link #copyDatabaseAsync(DatabaseCopyListener)}, so {@link #onCreate} creates
 * no tables. Schema changes are applied in {@link #onUpgrade}, keyed on the version number.
 *
 * <p>The queries live in one DAO per table group ({@link UserDao}, {@link ClassDao},
 * {@link EnrollmentDao}, {@link AttendanceDao}, {@link NotificationDao}, {@link SessionDao},
 * {@link RoomDao}, {@link QrCodeDao}). The public methods below only delegate to them so existing
 * callers are unchanged; new code can call the DAOs directly via {@link #users()} and friends.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "SmartClassroom.db";  // Database name as Qattendance.db
    private static final int DATABASE_VERSION = 3;
    private final Context context;
    private static String DATABASE_PATH = "/data/data/com.sdstiwi.gatekeeper/databases/";  // Database path
    private SQLiteDatabase db;

    private final UserDao users;
    private final ClassDao classes;
    private final EnrollmentDao enrollments;
    private final AttendanceDao attendance;
    private final NotificationDao notifications;
    private final SessionDao sessions;
    private final RoomDao rooms;
    private final QrCodeDao qrCodes;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        appContext = this.context;
        this.users = new UserDao(this);
        this.classes = new ClassDao(this);
        this.enrollments = new EnrollmentDao(this);
        this.attendance = new AttendanceDao(this);
        this.notifications = new NotificationDao(this);
        this.sessions = new SessionDao(this);
        this.rooms = new RoomDao(this);
        this.qrCodes = new QrCodeDao(this);
    }

    private static Context appContext;

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Database is preloaded from assets, so we don't create tables here.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Version 2: normalize legacy text timestamps to epoch milliseconds.
        if (oldVersion < 2) {
            db.execSQL("UPDATE sessions SET startTime = CAST(strftime('%s', startTime) AS INTEGER) * 1000 WHERE typeof(startTime) = 'text' AND startTime GLOB '????-??-??*'");
            db.execSQL("UPDATE sessions SET endTime = CAST(strftime('%s', endTime) AS INTEGER) * 1000 WHERE typeof(endTime) = 'text' AND endTime GLOB '????-??-??*'");
            db.execSQL("UPDATE qr_codes SET createdAt = CAST(strftime('%s', createdAt) AS INTEGER) * 1000 WHERE typeof(createdAt) = 'text' AND createdAt GLOB '????-??-??*'");
            db.execSQL("UPDATE qr_codes SET expiresAt = CAST(strftime('%s', expiresAt) AS INTEGER) * 1000 WHERE typeof(expiresAt) = 'text' AND expiresAt GLOB '????-??-??*'");
        }

        // Version 3: harden uniqueness safely. Older installations may already
        // contain duplicate rows, so the duplicates must be removed BEFORE
        // creating unique indexes. Otherwise the app crashes during startup.
        if (oldVersion < 3) {
            db.beginTransaction();
            try {
                // Keep the earliest enrollment for each student/class pair.
                db.execSQL("DELETE FROM enrollments WHERE enrollmentId NOT IN (" +
                        "SELECT MIN(enrollmentId) FROM enrollments GROUP BY studentId, classId)");

                // Keep the earliest attendance record for each student/session pair.
                db.execSQL("DELETE FROM attendance WHERE rowid NOT IN (" +
                        "SELECT MIN(rowid) FROM attendance GROUP BY userId, sessionId)");

                // Keep the earliest account for duplicate email addresses.
                // The application already performs a case-insensitive check on registration.
                db.execSQL("DELETE FROM users WHERE rowid NOT IN (" +
                        "SELECT MIN(rowid) FROM users GROUP BY email)");

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_attendance_student_session " +
                        "ON attendance(userId, sessionId)");
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_enrollment_student_class " +
                        "ON enrollments(studentId, classId)");
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email)");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Copies the bundled database from assets to internal storage if it is not there yet, then
     * checks that SQLite can open it. Runs on a background thread.
     *
     * @param listener called with {@code true} when the database is ready, {@code false} otherwise
     */
    public void copyDatabaseAsync(final DatabaseCopyListener listener) {
        new Thread(() -> {
            boolean success = false;
            try {
                File dbFile = new File(DATABASE_PATH + DATABASE_NAME);
                File parent = dbFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                if (!dbFile.exists()) {
                    try (InputStream inputStream = context.getAssets().open(DATABASE_NAME);
                         OutputStream outputStream = new FileOutputStream(dbFile)) {

                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.flush();
                    }
                    Log.d(TAG, "Bundled demo database copied successfully.");
                } else {
                    Log.d(TAG, "Database already exists at: " + dbFile.getAbsolutePath());
                }

                // Validate that SQLite can open the copied file.
                SQLiteDatabase check = SQLiteDatabase.openDatabase(
                        dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                success = check != null && check.isOpen();
                if (check != null) check.close();

            } catch (Exception e) {
                Log.e(TAG, "Error preparing demo database: " + e.getMessage(), e);
            }

            final boolean result = success;
            new Handler(context.getMainLooper()).post(() -> listener.onDatabaseCopied(result));
        }).start();
    }

    public interface DatabaseCopyListener {
        void onDatabaseCopied(boolean success);
    }

    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
            Log.d(TAG, "Database closed.");
        }
    }

    /**
     * Opens (or returns the already open) read/write handle to the copied database file.
     * Returns {@code null} if the file cannot be opened.
     */
    public SQLiteDatabase openDatabase() {
        if (db == null || !db.isOpen()) {
            Log.d(TAG, "Opening database...");
            try {
                db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
                Log.d(TAG, "Database opened successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error opening database: " + e.getMessage(), e);
            }
        }
        return db;
    }

    // Check if the database is open
    public boolean isDatabaseOpened() {
        return db != null && db.isOpen();
    }

    public String getTodayDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    // ---- Accessors for the per-table DAOs ----

    public UserDao users() {
        return users;
    }

    public ClassDao classes() {
        return classes;
    }

    public EnrollmentDao enrollments() {
        return enrollments;
    }

    public AttendanceDao attendance() {
        return attendance;
    }

    public NotificationDao notifications() {
        return notifications;
    }

    public SessionDao sessions() {
        return sessions;
    }

    public RoomDao rooms() {
        return rooms;
    }

    public QrCodeDao qrCodes() {
        return qrCodes;
    }

    // ---- Delegating methods (unchanged public API) ----

    public boolean insertOrUpdateUser(String uid, String email, String password, String firstName, String lastName, String speciality, String role, String department, String approvalStatus, int isActive) {
        return users.insertOrUpdateUser(uid, email, password, firstName, lastName, speciality, role, department, approvalStatus, isActive);
    }

    public boolean updateLastLogin(String uid) {
        return users.updateLastLogin(uid);
    }

    public Users getUserByEmail(String email) {
        return users.getUserByEmail(email);
    }

    public Users getUserByUid(String uid) {
        return users.getUserByUid(uid);
    }

    public Users getUserProfile(String uid) {
        return users.getUserProfile(uid);
    }

    public List<Users> getAllUsers() {
        return users.getAllUsers();
    }

    public void insertOrUpdateUser1(Users user) {
        users.insertOrUpdateUser1(user);
    }

    public Cursor getAllClasses() {
        return classes.getAllClasses();
    }

    public ClassModel getClassById(String classId) {
        return classes.getClassById(classId);
    }

    public List<Classes> getAllClasses1() {
        return classes.getAllClasses1();
    }

    public void insertOrUpdateClass(Classes cls) {
        classes.insertOrUpdateClass(cls);
    }

    public static List<ClassItem> getTeacherClasses(String teacherId) {
        if (appContext == null) return new ArrayList<>();
        return new DatabaseHelper(appContext).classes.getTeacherClasses(teacherId);
    }

    public Cursor getEnrolledStudents(String classId) {
        return enrollments.getEnrolledStudents(classId);
    }

    public boolean enrollStudent(String studentId, String classId, String teacherId) {
        return enrollments.enrollStudent(studentId, classId, teacherId);
    }

    public String getEnrollmentId(String studentId, String classId) {
        return enrollments.getEnrollmentId(studentId, classId);
    }

    public boolean updateEnrollmentStatus(String enrollmentId, String newStatus, String studentId, ClassModel classModel) {
        return enrollments.updateEnrollmentStatus(enrollmentId, newStatus, studentId, classModel);
    }

    public List<Enrollement> getAllEnrollments() {
        return enrollments.getAllEnrollments();
    }

    public void insertOrUpdateEnrollment(Enrollement model) {
        enrollments.insertOrUpdateEnrollment(model);
    }

    public static List<Enrollement> getStudentEnrollments(String studentId) {
        if (appContext == null) return new ArrayList<>();
        return new DatabaseHelper(appContext).enrollments.getStudentEnrollments(studentId);
    }

    public boolean insertAttendance(String userId, String sessionId, String qrCodeId) {
        return attendance.insertAttendance(userId, sessionId, qrCodeId);
    }

    public String recordAttendance(String studentId, String sessionId) {
        return attendance.recordAttendance(studentId, sessionId);
    }

    public List<AttendanceModel> getAllAttendanceRecords() {
        return attendance.getAllAttendanceRecords();
    }

    public void insertAttendanceIntoLocalDB(Context context, String attendanceId, String userId, String sessionId, String qrCodeId, long markedAt, String deviceInfo) {
        attendance.insertAttendanceIntoLocalDB(context, attendanceId, userId, sessionId, qrCodeId, markedAt, deviceInfo);
    }

    public boolean isNotificationAlreadySent(String receiverId, String classId, String status) {
        return notifications.isNotificationAlreadySent(receiverId, classId, status);
    }

    public void insertNotification(NotificationModel notification) {
        notifications.insertNotification(notification);
    }

    public List<NotificationModel> getUserNotifications(String receiverId) {
        return notifications.getUserNotifications(receiverId);
    }

    public void markNotificationAsRead(String notificationId) {
        notifications.markNotificationAsRead(notificationId);
    }

    public void markNotificationsAsReadAll(String userId) {
        notifications.markNotificationsAsReadAll(userId);
    }

    public Cursor getLatestNotifications(String studentId, int limit) {
        return notifications.getLatestNotifications(studentId, limit);
    }

    public List<NotificationModel> getAllNotifications() {
        return notifications.getAllNotifications();
    }

    public void insertOrUpdateNotification(NotificationModel notification) {
        notifications.insertOrUpdateNotification(notification);
    }

    public List<Session> getTodaysScheduleForTeacher(String teacherId) {
        return sessions.getTodaysScheduleForTeacher(teacherId);
    }

    public List<Session> getWeeklyTimetableForTeacher(String teacherId) {
        return sessions.getWeeklyTimetableForTeacher(teacherId);
    }

    public List<Session> getStudentClassesForToday(String studentId) {
        return sessions.getStudentClassesForToday(studentId);
    }

    public List<Session> getWeeklyScheduleForStudent(String studentId) {
        return sessions.getWeeklyScheduleForStudent(studentId);
    }

    public List<Room> getRoomsToTeachInToday(String teacherId) {
        return rooms.getRoomsToTeachInToday(teacherId);
    }

    public List<Room> getRoomsToTakeToday(String studentId) {
        return rooms.getRoomsToTakeToday(studentId);
    }

    public List<Room> getAllRooms() {
        return rooms.getAllRooms();
    }

    public void insertOrUpdateRoom(Room room) {
        rooms.insertOrUpdateRoom(room);
    }

    public void updateRoomStatuses(SQLiteDatabase db) {
        rooms.updateRoomStatuses(db);
    }

    public void logSQLiteNow(SQLiteDatabase db) {
        rooms.logSQLiteNow(db);
    }

    public List<QRCodeModel> getAllQRCodes() {
        return qrCodes.getAllQRCodes();
    }

    public List<QRCodeModel> getAllQRCodesFromLDB() {
        return qrCodes.getAllQRCodesFromLDB();
    }
}
