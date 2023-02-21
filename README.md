# gdx-scopedstorage

On the `:android` module, add the following dependencies
`build.gradle` dependencies for module 
```groovy
implementation "com.github.kurante2801:libgdxscopedstorage:0.1-SNAPSHOT"
implementation "androidx.documentfile:documentfile:1.0.1"
```

Add these two lines to your `gradle.properties` file
```properties
android.useAndroidX=true
android.enableJetifier=true
```

Set the `minSdkVersion` of Android's `build.gradle` to 21
```groovy
android {
	defaultConfig {
        applicationId "com.kurante.scopedtest"
        minSdkVersion 21 // <- This is what you need to change
        targetSdkVersion 33
        versionCode 1
        versionName "1.0"
    }
}
```