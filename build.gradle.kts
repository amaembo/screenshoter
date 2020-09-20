group = "one.util.ideaplugin"
version = "1.5-SNAPSHOT"

plugins {
    java
    id("org.jetbrains.intellij") version "0.4.22"
}

repositories {
    jcenter()
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
        resources {
            srcDir("resources")
        }
    }
}

intellij {
    pluginName = "screenshoter"

    type = "IU"
    version = "2020.2.2"
}

tasks {
    runIde {
        jvmArgs(
            "-Xms512m",
            "-Xmx2g"
        )
    }

    patchPluginXml {
        sinceBuild("191.0")
    }

    publishPlugin {
        setToken(project.findProperty("intellij.publish.token"))
    }

    wrapper {
        gradleVersion = "6.6.1"
        distributionType = Wrapper.DistributionType.ALL
    }
}
