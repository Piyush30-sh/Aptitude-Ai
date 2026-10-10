# Aptitude AI Pro

A Java Swing-based aptitude learning application with topic practice, analytics, performance tracking, and recommendation features.

## Features
- Topic-based aptitude question practice
- Difficulty-based scoring and weak-area analysis
- Personalized study recommendations
- Persistent local statistics and settings
- Desktop app experience with Swing

## Run locally
From the project folder:

```bash
mvn clean compile
mvn exec:java
```

## Project structure
- `src/main/java` — application source code
- `src/main/resources` — images, sound, and supporting assets
- `aptitude_data/` — local progress and analytics data
- `aptitude_settings.properties` — user preferences

## GitHub safety notes
This project keeps local user data in `aptitude_data/` and `aptitude_settings.properties`, which are ignored by the repository configuration to prevent uploading personal settings and progress.
