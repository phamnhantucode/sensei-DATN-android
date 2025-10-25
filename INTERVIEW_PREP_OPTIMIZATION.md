# Interview Prep Optimization

## Overview
This document describes the optimization implemented to reduce AI API calls and improve performance for the Interview Prep feature using **local device storage** instead of cloud database.

## Problem
Previously, every time a user entered the Interview Prep screen:
- Gemini API was called to generate questions (expensive & slow)
- Only 5 quiz questions and 4 interview questions were generated
- Questions were regenerated even if the user didn't complete them
- High cost and poor user experience due to loading times

## Solution: Local Question Pool Caching System

### Architecture

#### 1. Room Database (Local Storage)
A local Room database stores pre-generated questions on the device:
- **Entity**: `QuestionPoolEntity` - stores all question data
- **DAO**: `QuestionPoolDao` - provides query methods
- **Database**: `AppDatabase` - manages the Room database
- **Benefits**:
  - No network calls for cached questions
  - Instant loading (<10ms)
  - Works offline
  - No server costs

#### 2. Data Layer

**Room Entities** (`data/local/`):
- `QuestionPoolEntity.kt`: Database entity with fields for question data and usage tracking
- `QuestionPoolDao.kt`: Data Access Object with suspend functions for async operations
- `AppDatabase.kt`: Room database singleton

**Repository Methods** (`InterviewPrepRepository.kt`):
- `getUnusedQuestionsCount()`: Queries local database for unused question count
- `fetchUnusedQuestionsFromPool()`: Retrieves questions from Room database
- `storeQuestionsInPool()`: Saves generated questions locally
- `markQuestionsAsUsed()`: Updates local database to mark questions as used

**Batch Generation Logic:**
```kotlin
// Configuration
MINIMUM_POOL_SIZE = 10        // Trigger generation when below this
BATCH_QUIZ_SIZE = 50          // Generate 50 quiz questions per batch
BATCH_INTERVIEW_SIZE = 20     // Generate 20 interview questions per batch
QUIZ_QUESTIONS_PER_SESSION = 5
INTERVIEW_QUESTIONS_PER_SESSION = 4
```

**Flow:**
1. When loading content, check local pool size for both quiz and interview questions
2. If pool < MINIMUM_POOL_SIZE, generate a batch using Gemini
3. Store generated questions in local Room database
4. Load QUESTIONS_PER_SESSION from local pool (instant <10ms query)
5. After user completes quiz/interview, mark those questions as used in local database
6. Next time user enters, load new questions from existing local pool (no network/AI call)

#### 3. ViewModel Layer (`InterviewPrepViewModel.kt`)

**Changes:**
- Changed from `ViewModel` to `AndroidViewModel` to access Application context
- Repository now receives context for Room database initialization
- Modified `completeQuiz()` and `completeInterview()` to mark questions as used in local database
- Changed `refreshContent(force = false)` to use locally cached questions by default
- Questions are loaded from local database instead of generating new ones each time

### Benefits

1. **Cost Reduction**: ~90% reduction in Gemini API calls
   - Previously: 1 API call per screen entry
   - Now: 1 API call per ~10 quiz sessions (when pool runs low)

2. **Performance**: Instant loading instead of 2-10 second wait
   - Local database queries are <10ms
   - AI generation was 2-10 seconds
   - No network latency

3. **Better UX**:
   - No loading screen when entering Interview Prep (instant)
   - Continuous flow of fresh questions
   - Background batch generation
   - Works offline after initial setup

4. **Privacy**: Questions stored locally on device, not in cloud

5. **Reliability**: No dependency on network connection for cached questions

### User Flow

```
User enters Interview Prep
    ↓
Check local pool: 45 unused quiz questions, 15 unused interview questions (from Room DB)
    ↓
Load 5 quiz questions from local pool (instant <10ms)
    ↓
User completes quiz
    ↓
Mark 5 questions as used in local database
Local pool now has: 40 unused quiz questions
    ↓
User starts another quiz
    ↓
Load 5 more questions from local pool (instant <10ms)
    ↓
... repeat ...
    ↓
Local pool drops below 10 questions
    ↓
Background: Generate 50 new questions via Gemini
    ↓
Store in local Room database
    ↓
User continues with instant loading from local storage
```

### Setup

No additional setup required! The Room database is automatically created when the app first runs.

**Files added:**
- `app/src/main/java/com/phamnhantucode/aicareercoach/data/local/QuestionPoolEntity.kt`
- `app/src/main/java/com/phamnhantucode/aicareercoach/data/local/QuestionPoolDao.kt`
- `app/src/main/java/com/phamnhantucode/aicareercoach/data/local/AppDatabase.kt`

**Dependencies added to `app/build.gradle.kts`:**
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

### Configuration

You can adjust the batch sizes and thresholds in `InterviewPrepRepository.kt`:

```kotlin
companion object {
    private const val MINIMUM_POOL_SIZE = 10        // When to trigger batch generation
    private const val BATCH_QUIZ_SIZE = 50          // How many quiz questions to generate
    private const val BATCH_INTERVIEW_SIZE = 20     // How many interview questions to generate
    private const val QUIZ_QUESTIONS_PER_SESSION = 5
    private const val INTERVIEW_QUESTIONS_PER_SESSION = 4
}
```

**Recommendations:**
- **Low traffic**: MINIMUM_POOL_SIZE = 5, smaller batches
- **High traffic**: MINIMUM_POOL_SIZE = 20, larger batches
- **Cost-sensitive**: Increase MINIMUM_POOL_SIZE to reduce API calls
- **Variety-focused**: Decrease batch sizes for more frequent regeneration

### Monitoring

Key metrics to track:
1. **Pool utilization**: How many questions in pool vs. used
2. **Generation frequency**: How often batches are triggered
3. **User retention**: Time between quiz attempts
4. **Cost savings**: API calls before vs. after

### Future Enhancements

1. **Smart Pre-generation**: Generate questions based on user activity patterns
2. **Difficulty Progression**: Adjust question difficulty based on user performance
3. **Topic Rotation**: Ensure diverse topic coverage in pool
4. **Expiration**: Auto-expire old questions to keep content fresh
5. **Analytics**: Track which questions are most challenging
6. **A/B Testing**: Test different pool sizes and batch configurations

### Rollback

If you need to rollback to the previous behavior:
1. Set `MINIMUM_POOL_SIZE = 0` to disable pool checking
2. Or comment out the pool logic and revert to direct Gemini calls

### Testing

To test the implementation:
1. Clear app data or uninstall/reinstall to start fresh
2. Enter Interview Prep for the first time (will generate initial batch and store locally)
3. Complete 5-10 quizzes (should load instantly from local pool)
4. Monitor logcat for database operations vs Gemini API calls
5. Try airplane mode - cached questions should still load instantly

**Expected behavior:**
- First entry: 2-10 second load (generating questions)
- Subsequent entries: Instant (<10ms from local database)
- After ~10 quizzes: Brief wait while generating new batch in background
