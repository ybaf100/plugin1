// Paper 26.3 용 플러그인. shared 소스를 이 버전 API로 다시 컴파일하고, core를 jar에 함께 넣는다.
plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        java.srcDir("../shared/src/main/java")
        resources.srcDir("../shared/src/main/resources")
    }
}

dependencies {
    implementation(project(":core"))
    compileOnly("io.papermc.paper:paper-api:26.3.build.259-stable")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

val coreJar = project(":core").tasks.named<Jar>("jar")

tasks.jar {
    archiveFileName.set("WeaponSystem-26.3-${project.version}.jar")
    dependsOn(coreJar)
    from(coreJar.map { zipTree(it.archiveFile) })
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xlint:deprecation,removal")
}
