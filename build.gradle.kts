// Версии плагинов централизованы в settings.gradle.kts (pluginManagement.plugins),
// а не здесь: применение Kotlin-плагина в корневом build.gradle.kts (даже через
// apply false) ломает видимость классов AGP для подмодулей — см. KT-31643.
