# GateKeeper — full portfolio demo walkthrough

The bundled Demo Mode resets to a deterministic dataset every time the login database is prepared. This lets you demonstrate the complete workflow without Firebase credentials, internet access, an ESP32, or a Bluetooth controller.

## Demo accounts

| Role | Email | Password | Purpose |
|---|---|---|---|
| Teacher | `teacher1@example.com` | `password123` | Full teacher workflow |
| Student | `student1@example.com` | `password123` | Full student workflow |

Use the one-tap **Teacher Demo** / **Student Demo** buttons on Login.

## Scenario 1 — Teacher dashboard

Login as Teacher and show:

- 3 classes scheduled today
- 1 student already present in the live C1 attendance session
- 1 pending enrollment request
- 2 connected room controllers
- recent attendance and door-access activity

The dashboard is intentionally populated rather than empty so the first screen immediately communicates what the system does.

## Scenario 2 — Weekly timetable

Open **Timetable**.

The demo contains multiple weekday/time combinations, including:

- Distributed Systems — today + Friday — 09:00–10:30
- Network Security — Thursday — 11:00–12:00
- Cloud Computing — today + Monday — 14:00–15:30
- Mobile Application Development — Wednesday + Friday — 11:00–12:00

This demonstrates multi-day scheduling, different rooms, and both morning/afternoon slots.

## Scenario 3 — Live attendance

Open **Attendance**.

The database contains:

- an active Distributed Systems session with one existing attendance record
- an active Cloud Computing session with no attendance yet
- a historical attendance record
- device information and timestamps

As Student Demo, use the Cloud Computing QR to demonstrate a successful check-in. Use Distributed Systems to demonstrate duplicate-check protection.

This demonstrates both an active session and previously stored attendance data.

## Scenario 4 — QR workflow

The demo contains real generated QR bitmap data for active attendance sessions.

As a Student, open the attendance/QR screen and show the active QR cards. The student is enrolled in the corresponding classes, so the QR workflow has realistic context instead of an empty state.

The dataset also contains an expired QR record to represent the lifecycle of a session QR.

## Scenario 5 — Enrollment workflow

As Teacher, open **Notifications**.

There is an unread enrollment request from **Emma Wilson** for Distributed Systems. The notification exposes the existing **Approve** and **Reject** actions.

Try one action during the demo. GateKeeper updates the enrollment state and creates a reply notification for the student.

Additional seeded records represent:

- approved enrollment
- pending enrollment
- rejected enrollment
- already-read notification
- unread notification

## Scenario 6 — Student experience

Logout and enter **Student Demo**.

Show:

- 3 classes today
- approved enrolled classes
- active QR attendance sessions
- QR check-in success and duplicate protection
- upcoming-class notification
- enrollment reply notification
- profile information
- weekly timetable

## Scenario 7 — Room/access states

The dataset includes:

- Room 101 — connected/authorized
- Room 102 — connected/authorized
- Lab 201 — closed/inactive

Access logs also contain both successful and denied examples. This makes the room-control concept visible even though the default portfolio build does not require physical ESP32 hardware.

## Scenario 8 — Empty/error/edge cases represented in the data

The project also retains records for:

- inactive user account
- pending account approval
- inactive class
- duplicate enrollment attempt
- expired QR code
- rejected enrollment
- denied room access
- read and unread notifications

These records are useful when explaining how the application handles real-world state changes.

## Suggested 3-minute presentation order

1. Login → Teacher Demo
2. Dashboard → explain the 3/2/1 summary cards
3. Timetable → show the week
4. Attendance → show present students
5. Notifications → approve/reject an enrollment request
6. Logout → Student Demo
7. Student dashboard → show QR-ready classes
8. QR/attendance → show active session
9. Notifications → show confirmation/reply
10. Profile → close with the architecture and hardware/cloud extensions

## Portfolio talking point

> “The application is intentionally offline-first for the demonstration. The local SQLite dataset reproduces the important real-world states — active sessions, attendance, enrollment requests, notifications, QR expiry, room access and account approval — while the original Firebase, Bluetooth and ESP32 integration remains available as the connected deployment path.”
