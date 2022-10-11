@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.mpp)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.publish.jfrog)
    `maven-publish`
}
/**
 * Tracks the [LibraryVariant.DEBUG] version number designated for all targets. Some targets,
 * especially JS, require that each published snapshot must have an incremented version number.
 * The version follows the [semantic versioning](https://semver.org/) scheme.
 * @see libraryVariant
 */
val debugVersion = "0.0.0"

/**
 * Tracks the [LibraryVariant.OFFICIAL] version number designated for all targets. Some targets,
 * especially JS, require that each published snapshot must have an incremented version number.
 * The version follows the [semantic versioning](https://semver.org/) scheme.
 * @see libraryVariant
 */
val officialVersion = "0.0.0"

/**
 * The build script uses this constant to configure [Artifactory](https://flyng.jfrog.io/) publishing
 * intention for all targets.
 * @see LibraryVariant
 */
val libraryVariant = LibraryVariant.DEBUG

// constants
val scope = "flyng"
val rotorPublishGroup = "rotor publishing"

group = "com.$scope.${project.name}"
version = when (libraryVariant) {
    LibraryVariant.DEBUG -> debugVersion
    LibraryVariant.OFFICIAL -> officialVersion
}

/**
 * Enumerate artifact variants for all targets.
 * @see LibraryVariant.DEBUG
 * @see LibraryVariant.OFFICIAL
 */
enum class LibraryVariant {
    /**
     * Represents artifacts being under development or testing stage. All targets will
     * have for itself a dedicated [Artifactory](https://flyng.jfrog.io/) repository
     * to store continuously changing snapshots.
     * @see MavenVariant.SNAPSHOT
     * @see NpmVariant.DEVELOPMENT
     */
    DEBUG,

    /**
     * Represents artifacts that are ready to go to production. All targets will
     * have for itself a dedicated [Artifactory](https://flyng.jfrog.io/) repository
     * to store officially released snapshots.
     * @see MavenVariant.RELEASE
     * @see NpmVariant.PRODUCTION
     */
    OFFICIAL
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.jfrog.extractor)
    }
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    js(IR) {
        compilations["main"].packageJson {
            name = "@$scope/${project.name}"
            version = project.version as String
        }
        browser()
        binaries.library()
    }
    android()
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
                implementation(npm("filament", "=${libs.versions.filament.get()}"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting

        @Suppress("UNUSED_VARIABLE")
        val androidMain by getting {
            dependencies {
                implementation(libs.google.filament.android)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val androidTest by getting {
            dependencies {
                dependsOn(sourceSets["androidAndroidTestRelease"])
                dependsOn(sourceSets["androidTestFixtures"])
                dependsOn(sourceSets["androidTestFixturesDebug"])
                dependsOn(sourceSets["androidTestFixturesRelease"])
            }
        }
    }
}

@Suppress("UnstableApiUsage")
android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isJniDebuggable = true
        }
    }

    publishing {
        singleVariant("release")
        singleVariant("debug")
    }

    namespace = project.group as String
}

// register publish tasks
tasks {
    register("npmPublish") {
        val npmVariant = if (libraryVariant == LibraryVariant.DEBUG) NpmVariant.DEVELOPMENT else NpmVariant.PRODUCTION
        dependsOn(
            when (npmVariant) {
                NpmVariant.DEVELOPMENT -> "jsBrowserDevelopmentLibraryDistribution"
                NpmVariant.PRODUCTION -> "jsBrowserProductionLibraryDistribution"
            }
        )
        group = rotorPublishGroup
        description =
            "Build and publish JS target to Artifactory. The variant is decided by the libraryVariant variable."
        doLast {
            executeNpmPublishCommand(npmVariant)
        }
    }
    register("mavenPublish") {
        dependsOn("artifactoryPublish")
        group = rotorPublishGroup
        description =
            "Build and publish Android target to Artifactory. The variant is decided by the libraryVariant variable."
    }
    register("rotorPublish") {
        dependsOn("mavenPublish")
        dependsOn("npmPublish")
        group = rotorPublishGroup
        description =
            "Build and publish all targets to Artifactory. The variant is decided by the libraryVariant variable."
    }
}

// because Gradle automatically generates tasks according to each android publication,
// we name the enumerates with Pascal case
enum class AndroidPublication(val component: String) {
    AarDebug("debug"),
    AarRelease("release")
}

fun PublicationContainer.registerAarPublication(androidPublication: AndroidPublication) {
    // we directly use the enum name to designate the publication
    register(androidPublication.name, MavenPublication::class.java) {
        groupId = project.group as String
        version = project.version as String
        artifactId = project.name

        // software components are not created right when the plugin is applied, they are
        // instead created during the afterEvaluate() callback step
        // the publication that selects the software component must also be configured
        // during the afterEvaluate() step.
        afterEvaluate {
            from(components[androidPublication.component])
        }
    }
}

publishing {
    publications {
        // register all aar publications for android artifactory
        AndroidPublication.values().forEach { registerAarPublication(it) }
    }
}

/**
 * Enumerate artifact variants for Android target.
 * @param repoKey the [Artifactory](https://flyng.jfrog.io/) repository key for this variant.
 * @param release whether this artifact is of type [LibraryVariant.OFFICIAL].
 * @param publication the name of the configured publication this variant will use to publish.
 * @see MavenVariant.SNAPSHOT
 * @see MavenVariant.RELEASE
 */
enum class MavenVariant(val repoKey: String, val release: Boolean, val publication: String) {
    /**
     * The script will automatically choose this variant for Android target when [libraryVariant]
     * is set to [LibraryVariant.DEBUG].
     */
    SNAPSHOT("rotor-snapshot-local", false, AndroidPublication.AarDebug.name),

    /**
     * The script will automatically choose this variant for Android target when [libraryVariant]
     * is set to [LibraryVariant.OFFICIAL].
     */
    RELEASE("rotor-release-local", true, AndroidPublication.AarRelease.name)
}

// configure maven publish task
artifactory {
    val mavenVariant =
        if (libraryVariant == LibraryVariant.DEBUG)
            MavenVariant.SNAPSHOT
        else
            MavenVariant.RELEASE

    // set the credentials for maven repo in ~/.gradle/gradle.properties
    val mavenUser: String by project
    val mavenPassword: String by project

    publish {
        // publish info
        repository {
            // publisher base Artifactory URL
            setContextUrl("https://flyng.jfrog.io/artifactory")
            setUsername(mavenUser)
            setPassword(mavenPassword)
            setRepoKey(mavenVariant.repoKey)
        }
        // publications
        defaults {
            publications(mavenVariant.publication)
        }
    }
    buildInfo {
        buildName = "${mavenVariant.repoKey}-$version"
        buildNumber = System.currentTimeMillis().toString(16)
        isReleaseEnabled = mavenVariant.release
    }
    with(clientConfig) {
        isIncludeEnvVars = true
        envVarsExcludePatterns = "*password*,*secret*"
        envVarsIncludePatterns = "*not-secret*"
    }
}

/**
 * Enumerate artifact variants for JS target.
 * @param repoKey the [Artifactory](https://flyng.jfrog.io/) repository key for this variant.
 * @param targetDirName the JS output build directory that this variant will use to publish.
 * @see NpmVariant.DEVELOPMENT
 * @see NpmVariant.PRODUCTION
 */
enum class NpmVariant(val repoKey: String, val targetDirName: String) {
    /**
     * The script will automatically choose this variant for JS target when [libraryVariant]
     * is set to [LibraryVariant.DEBUG].
     */
    DEVELOPMENT("rotor-development-local", "developmentLibrary"),

    /**
     * The script will automatically choose this variant for JS target when [libraryVariant]
     * is set to [LibraryVariant.OFFICIAL].
     */
    PRODUCTION("rotor-production-local", "productionLibrary")
}

fun executeNpmPublishCommand(variant: NpmVariant) {
    val registryUrl = "https://flyng.jfrog.io/artifactory/api/npm/${variant.repoKey}/"
    val packageDir = File("$buildDir/${variant.targetDirName}")
    // directory to node home
    val nodeDirectory = providers
        .environmentVariable("NODE_HOME")
        .orElse(providers.environmentVariable("NVM_SYMLINK"))
        .orNull?.let { layout.projectDirectory.dir(it) }
        ?: throw RuntimeException("Please set either NODE_HOME or NVM_SYMLINK environment variable.")
    // node executable
    val node = nodeDirectory.file(
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
            "node.exe"
        } else {
            "bin/node"
        }
    )
    // invoke npm through the npm-cli.js, not the npm on command line
    val npm = nodeDirectory.file(
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
            "node_modules/"
        } else {
            "lib/node_modules/"
        } + "npm/bin/npm-cli.js"
    )
    // change the npm repository registry
    exec {
        workingDir = packageDir
        executable = node.asFile.absolutePath
        args = listOf(
            npm.asFile.absolutePath,
            "config",
            "set",
            "@flyng:registry",
            registryUrl
        )
    }
    // publish
    exec {
        workingDir = packageDir
        executable = node.asFile.absolutePath
        args = listOf(
            npm.asFile.absolutePath,
            "publish",
            "--registry",
            registryUrl,
        )
    }
}