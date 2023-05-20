plugins {
    id("dev.architectury.loom") version "1.2.+"
}

loom {
    silentMojangMappingsLicense()
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    compileOnly(project(":test-bukkit"))

    minecraft("com.mojang:minecraft:1.16.5")

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.16.5:2022.03.06@zip")
    })

    forge("net.minecraftforge:forge:1.16.5-36.2.39")
}