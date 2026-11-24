# OOREP Android (Open Online Repertory for Mobile)

A modern, fast, and beautiful Android application for **OOREP (Open Online Repertory for Homeopathy)**.

## ✨ Highlights

- 🌿 **Material 3 (Material You)** design: Clean, modern, medical-grade typography and fluid animations.
- ⚡ **100% Offline Capability**: Built-in SQLite database indexing over 146,000 rubrics and 1.35 million rubric-remedy connections with sub-millisecond query responses.
- 🔍 **Instant Symptom & Rubric Search**: Multi-keyword filtering with chapter filters and remedy displays.
- 🎯 **Visual Remedy Weight Grading**:
  - **Grade 4**: Red / Scarlet badge
  - **Grade 3**: Bold Deep Indigo
  - **Grade 2**: Italic Medium Blue
  - **Grade 1**: Neutral Gray
- 📊 **Repertorization Engine & Case Management**:
  - Add rubrics to active patient cases.
  - Set custom symptom weights (1x, 2x, 3x, 4x) or toggle eliminating symptoms.
  - Real-time remedy scoring matrix ranked by rubric coverage and cumulative score.
- 📖 **Materia Medica Reader**: William Boericke's Pocket Manual of Homoeopathic Materia Medica directly linked to remedy profiles.
- ☁️ **Cloud Built with GitHub Actions**: All heavy PC tasks (Gradle build, database indexing, APK packaging) are run in GitHub Actions. Zero burden on local PC!

## 🚀 Downloading & Installing the APK

Download the latest pre-built `.apk` directly from:
- [GitHub Releases](https://github.com/dhyankrishna24/oorep-android/releases)
- Or GitHub Actions Workflow Artifacts under the **Actions** tab.

### One-Click Install with ADB
Run the included PowerShell script to download the latest build and install directly to your device:
```powershell
./scripts/install_apk.ps1
```

## 🛠 Project Architecture
- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture / MVVM with StateFlow
- **Database**: SQLite / Room (`assets/databases/oorep.db`)
- **CI/CD**: GitHub Actions (`.github/workflows/build-and-release.yml`)
