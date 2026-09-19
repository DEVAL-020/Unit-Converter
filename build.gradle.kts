plugins {
    kotlin("multiplatform") version "2.2.20"
}

group = "dev.unitconverter"
version = "1.0.0"

kotlin {
    
    jvm()

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "unit-converter.js"
            }
            // Browser tests need Chrome and Karma; the logic is tested on the JVM instead.
            testTask {
                enabled = false
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
