import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().also { properties ->
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(properties::load)
    }
}

val neonApiUrl =
    localProperties.getProperty(
        "NEON_API_URL",
        "https://ep-hidden-fire-a17d8p5n.apirest.ap-southeast-1.aws.neon.tech/neondb/rest/v1"
    )
val neonApiKey = localProperties.getProperty("NEON_API_KEY", "")
val neonDbRole = localProperties.getProperty("NEON_DB_ROLE", "")
val neonDbPassword = localProperties.getProperty("NEON_DB_PASSWORD", "")
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
val cldnrApiKey = localProperties.getProperty("CLDNR_API_KEY", "")
val cldnrApiSecret = localProperties.getProperty("CLDNR_API_SECRET", "")
val cldnrCloudName = localProperties.getProperty("CLDNR_CLOUD_NAME", "")
val cldnrUploadPreset = localProperties.getProperty("CLDNR_UPLOAD_PRESET", "")
val clerkPublishableKey = localProperties.getProperty("CLERK_PUBLISHABLE_KEY", "")
val clerkSecretKey = localProperties.getProperty("CLERK_SECRET_KEY", "")

android {
    namespace = "com.phamnhantucode.aicareercoach"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.phamnhantucode.aicareercoach"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "CLERK_PUBLISHABLE_KEY",
            "\"$clerkPublishableKey\""
        )
        buildConfigField(
            "String",
            "CLERK_SECRET_KEY",
            "\"$clerkSecretKey\""
        )
        buildConfigField("String", "NEON_API_URL", "\"$neonApiUrl\"")
        buildConfigField("String", "NEON_API_KEY", "\"$neonApiKey\"")
        buildConfigField("String", "NEON_DB_ROLE", "\"$neonDbRole\"")
        buildConfigField("String", "NEON_DB_PASSWORD", "\"$neonDbPassword\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "CLDNR_API_KEY", "\"$cldnrApiKey\"")
        buildConfigField("String", "CLDNR_API_SECRET", "\"$cldnrApiSecret\"")
        buildConfigField("String", "CLDNR_CLOUD_NAME", "\"$cldnrCloudName\"")
        buildConfigField("String", "CLDNR_UPLOAD_PRESET", "\"$cldnrUploadPreset\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${localProperties.getProperty("OPENROUTER_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation(libs.clerk.android)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Room for local database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager for reliable background sync
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Reorderable LazyColumn for drag-drop reordering (local module)
    implementation(project(":reorderable"))

    // Color picker library
    implementation(project(":colorpicker"))

    // Vosk offline speech recognition
    implementation("com.alphacephei:vosk-android:0.3.47")

    // PDFBox for PDF text extraction
    implementation(libs.pdfbox)

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Stripe Android SDK
    implementation("com.stripe:stripe-android:21.4.0")
}
