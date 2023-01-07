plugins {
    id("java")
}

group = "de.pierreschwang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://nexus.lichtspiele.org/repository/releases/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    implementation(platform("com.intellectualsites.bom:bom-1.18.x:1.20"))
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.plotsquared:PlotSquared-Core")
    compileOnly("com.nisovin.shopkeepers:ShopkeepersAPI:2.16.3")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.0")
}