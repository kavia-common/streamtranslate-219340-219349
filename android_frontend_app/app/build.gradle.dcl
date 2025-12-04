androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
        implementation("androidx.datastore:datastore-preferences:1.1.1")
        implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
        implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
        implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    }
}
