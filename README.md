# TakeExamApp

TakeExamApp is an Android application for creating and taking exams while recording the location where each exam is completed.

## Features

### Student

- Select a published exam and student name
- Record latitude, longitude and a short address using OpenStreetMap
- Complete open and multiple-choice questions
- Resume an interrupted exam
- Store the result, date and exam duration in Firestore

### Admin

- Sign in with Firebase Authentication
- Add and delete students
- Import multiple students through copy/paste
- Create, publish and delete exams
- Add open and multiple-choice questions
- Review open answers and award points
- View results by student and by exam
- View the exam location on an OpenStreetMap map

## Firebase administrator setup

Create an Email/Password user in Firebase Authentication. Copy that user's UID and create this Firestore document before publishing the included security rules:

```text
admins/{uid}
    enabled: true
```


## Demo

[Watch-the-TakeExam-demo-video](demo/2026-08-29 22-43-19.mkv)

The video demonstrates administrator login, user and exam management, taking and grading an exam, location capture, OpenStreetMap and the data stored in Firestore.
