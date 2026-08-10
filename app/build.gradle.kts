import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseSigningPropertiesFile = rootProject.layout.projectDirectory.file("keystore.properties")
val releaseSigningPropertiesText = providers
    .fileContents(releaseSigningPropertiesFile)
    .asText
    .orNull
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesText != null) {
        releaseSigningPropertiesText.reader().use(::load)
    }
}

fun requiredReleaseSigningProperty(name: String): String =
    releaseSigningProperties.getProperty(name)?.takeIf(String::isNotBlank)
        ?: error("Missing '$name' in ${releaseSigningPropertiesFile.asFile.path}")

android {
    namespace = "com.chemecador.passwordmemory"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.chemecador.passwordmemory"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Committed schemas are the reference every future migration is tested against; the assets
    // entry is what lets MigrationTestHelper open version 1 from an instrumented test.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }

    signingConfigs {
        if (releaseSigningPropertiesText != null) {
            create("release") {
                storeFile = rootProject.file(requiredReleaseSigningProperty("storeFile"))
                storePassword = requiredReleaseSigningProperty("storePassword")
                keyAlias = requiredReleaseSigningProperty("keyAlias")
                keyPassword = requiredReleaseSigningProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// room-testing reads the exported schemas with kotlinx-serialization, and its serializers were
// generated against core 1.8, where `typeParametersSerializers()` gained a default implementation.
// The Compose BOM strictly pins core to 1.7.3, where it is still abstract, so on a device every
// schema read dies with AbstractMethodError — invisible to the JVM tests. `force` is what overrides
// a `strictly`, and it has to apply to every configuration because AGP resolves the androidTest
// classpath consistently with the app's. The app itself never uses kotlinx-serialization: it only
// ever arrives transitively, so this moves nothing in production code.
configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:${libs.versions.kotlinxSerialization.get()}",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${libs.versions.kotlinxSerialization.get()}"
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// AGP already uses bundletool to produce App Bundles. This verification task deliberately reuses
// that exact plugin classpath instead of adding another build dependency or committing a binary.
// The release script disables the configuration cache and daemon before invoking it because the
// same project configuration may also load upload-key credentials.
val bundleToValidate = providers.gradleProperty("passwordMemory.bundleToValidate")
tasks.register("validatePlayBundle") {
    group = "verification"
    description = "Semantically validates an Android App Bundle with AGP's bundletool."
    mustRunAfter("bundleRelease")
    notCompatibleWithConfigurationCache("Invokes AGP's bundled bundletool reflectively.")

    doLast {
        val configuredPath = bundleToValidate.orNull
            ?: error("Pass -PpasswordMemory.bundleToValidate=<absolute-aab-path>.")
        val bundlePath = Path.of(configuredPath).toAbsolutePath().normalize()
        check(Files.isRegularFile(bundlePath)) {
            "Bundle to validate does not exist: $bundlePath"
        }

        val classLoader = Thread.currentThread().contextClassLoader
        val commandType = classLoader.loadClass(
            "com.android.tools.build.bundletool.commands.ValidateBundleCommand"
        )
        val builderType = classLoader.loadClass(
            "com.android.tools.build.bundletool.commands.ValidateBundleCommand\$Builder"
        )
        val builder = commandType.getMethod("builder").invoke(null)
        builderType.getMethod("setBundlePath", Path::class.java).invoke(builder, bundlePath)
        builderType.getMethod(
            "setPrintOutput",
            Boolean::class.javaPrimitiveType!!
        ).invoke(builder, false)
        val command = builderType.getMethod("build").invoke(builder)

        try {
            commandType.getMethod("execute").invoke(command)
        } catch (exception: InvocationTargetException) {
            throw GradleException(
                "bundletool rejected $bundlePath",
                exception.targetException ?: exception
            )
        }
    }
}
