plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.google.devtools.ksp)
	alias(libs.plugins.dagger.hilt.android)
}

android {
	namespace = "dev.kuylar.ownstream.tvleanback"
	compileSdk {
		version = release(36) {
			minorApiLevel = 1
		}
	}

	defaultConfig {
		applicationId = "dev.kuylar.ownstream.tvleanback"
		minSdk = 23
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

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
	buildFeatures {
		viewBinding = true
		buildConfig = true
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.leanback)
	implementation(project(":ownstream:api"))
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.glide)
	implementation(libs.dagger.hilt.android)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	ksp(libs.dagger.hilt.compiler)
	implementation(libs.media3.exoplayer)
	implementation(libs.media3.exoplayer.hls)
	implementation(libs.media3.ui)
}