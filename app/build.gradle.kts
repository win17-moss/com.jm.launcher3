import com.android.build.gradle.internal.tasks.getTestOnlyNativeLibs

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jm.launcher3"
    compileSdk = 34
//    defaultConfig {
//        manifestPlaceholders = mapOf(
//            "testOnly" to "false"
//        )
//    }

    defaultConfig {
        applicationId = "com.jm.launcher3"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 直接修改 manifestPlaceholders 的内容
        manifestPlaceholders.put("testOnly", "false")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 添加签名配置
    signingConfigs {
        create("release") {
            // 密钥库文件路径
            storeFile = file("h:/key/cdptsjm.jks")
            // 密钥库密码
            storePassword = "1145141478"
            // 密钥别名
            keyAlias = "1145141478"
            // 密钥密码
            keyPassword = "1145141478"
        }
    }

    // 将签名配置应用于 release 构建类型
    buildTypes.getByName("release") {
        signingConfig = signingConfigs.getByName("release")
    }

    // 添加任务来修改 AndroidManifest.xml
    tasks.register("removeTestOnly") {
        doLast {
            val manifestFile = File("H:\\14\\app\\src\\main\\AndroidManifest.xml")
            val manifestContent = manifestFile.readText()
            val updatedContent = manifestContent.replace("android:testOnly=\"true\"", "android:testOnly=\"false\"")
            manifestFile.writeText(updatedContent)
        }
    }

    // 在 preBuild 任务之前运行 removeTestOnly 任务
    tasks.getByName("preBuild") {
        dependsOn("removeTestOnly")
    }

//    removeTestOnlyFlag(context, "com.example.app", "com.example.app.MainActivity");
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    releaseImplementation("com.huawei.hms:hemsdk:5.0.3.300")
    releaseImplementation("com.huawei.mdm:mdmkit:13.1.0.300")
}