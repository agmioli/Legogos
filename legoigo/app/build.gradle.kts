import java.io.File
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
}

// =========================================================================
// ИЗВЛЕЧЕНИЕ НАТИВНЫХ .SO В ОБЫЧНУЮ ПАПКУ (НЕ Provider!)
// =========================================================================

val natives: Configuration = configurations.create("natives") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// ВАЖНО: обычный File, а не Provider<Directory>
val jniLibsExtractDir: File = project.file("build/generated/jniLibs")

val extractNatives = tasks.register("extractNatives") {
    // Пути ко всем JAR-ам
    val jarFiles = natives.files

    inputs.files(jarFiles)
    outputs.dir(jniLibsExtractDir)

    doLast {
        if (jniLibsExtractDir.exists()) jniLibsExtractDir.deleteRecursively()
        jniLibsExtractDir.mkdirs()

        jarFiles.forEach { jar ->
            val abi = when {
                jar.name.contains("natives-armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("natives-arm64-v8a") -> "arm64-v8a"
                jar.name.contains("natives-x86_64") -> "x86_64"
                jar.name.contains("natives-x86") -> "x86"
                else -> null
            }
            if (abi == null) {
                logger.lifecycle("extractNatives: skip ${jar.name}")
                return@forEach
            }

            val abiDir = File(jniLibsExtractDir, abi)
            abiDir.mkdirs()

            ZipFile(jar).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".so")) {
                        val fileName = entry.name.substringAfterLast('/')
                        val target = File(abiDir, fileName)
                        zip.getInputStream(entry).use { input ->
                            target.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        logger.lifecycle("extractNatives -> $abi/$fileName")
                    }
                }
            }
        }
    }
}

// Задача обязательно должна выполниться ДО preBuild
tasks.named("preBuild").configure {
    dependsOn(extractNatives)
}

// =========================================================================
// ANDROID-КОНФИГУРАЦИЯ
// =========================================================================

android {
    namespace = "com.example.legoigo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.legoigo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
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

    buildFeatures {
        viewBinding = true
    }

    // Привязываем обычную папку (File) к jniLibs
    sourceSets {
        getByName("main") {
            jniLibs.srcDir(jniLibsExtractDir)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// =========================================================================
// ЗАВИСИМОСТИ
// =========================================================================

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-bullet:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-freetype:1.12.1")

    add("natives", "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")

    add("natives", "com.badlogicgames.gdx:gdx-bullet-platform:1.12.1:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-bullet-platform:1.12.1:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-bullet-platform:1.12.1:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-bullet-platform:1.12.1:natives-x86_64")

    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:1.12.1:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:1.12.1:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:1.12.1:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:1.12.1:natives-x86_64")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}