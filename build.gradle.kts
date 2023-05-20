allprojects {
    apply(plugin = "java-library")

    group = "enterprises.stardust"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://maven.minecraftforge.net/") {
            content {
                includeGroup("cpw.mods")
            }
        }
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            content {
                includeGroup("org.spigotmc")
                includeGroup("org.test")
            }
        }
        maven("https://jitpack.io/") {
            content {
                includeGroup("com.github.xtrm-en")
            }
        }
    }

    val shade by configurations.creating {
        val implementation by configurations
        implementation.extendsFrom(this)
    }

    dependencies {
        "implementation"("org.jetbrains", "annotations", "24.0.1")
        // javadoc breaks without this apparently
        "implementation"("com.google.code.findbugs", "jsr305", "3.0.2")

        "implementation"("cpw.mods", "modlauncher", "8.1.3")
        "implementation"("org.spigotmc", "spigot-api", "1.16.+")

        if (project == rootProject) {
            "implementation"("org.apache.logging.log4j", "log4j-api", "2.14.1")
            "shade"("com.github.xtrm-en", "deencapsulation", "42b829f373")
        }
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    tasks {
        getByName<Jar>("jar") {
            from(shade.map { if (it.isDirectory) it else zipTree(it) })
        }
    }
}