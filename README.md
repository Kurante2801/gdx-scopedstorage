# gdx-scopedstorage

Implements the class `DocumentHandle` that extends from `FileHandle` and calls the `DocumentFile` API.

## Installation

Inside your main `build.gradle` add the following dependencies for the module `:android`
```groovy
implementation "com.github.kurante2801:gdx-scopedstorage:1.0.0"
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

## Usage
Inside your `AndroidLauncher` class define a `ScopedStorageHandler`, create it in the `onCreate` function, 
then override `onActivityResult` to also call `scopedHandler.onActivityResult`:<br>

### Kotlin:
```kotlin
class AndroidLauncher : AndroidApplication() {
    private lateinit var scopedHandler: ScopedStorageHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration()
        scopedHandler = ScopedStorageHandler(this) // Handler creation
        initialize(ScopedTest(), config)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scopedHandler.onActivityResult(requestCode, resultCode, data)
    }
}
```
### Java
```java
public class AndroidLauncher extends AndroidApplication {
    private ScopedStorageHandler scopedHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        scopedHandler = new ScopedStorageHandler(this); // Handler creation
        initialize(new ScopedTest(), config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        scopedHandler.onActivityResult(requestCode, resultCode, data);
    }
}

```

Finally, to ask the user to select a directory simply call `scopedHandler.requestDocumentTree` (see [Interfacing with platform specific code](https://libgdx.com/wiki/app/interfacing-with-platform-specific-code))<br>
### Kotlin
```kotlin
val makePersistent = true
scopedHandler.requestDocumentTree(makePersistent) { handle ->
    // handle may be null if user cancelled or the request ended in failure
}
```

### Java
```java
boolean makePersistent = true;
scopedHandler.requestDocumentTree(makePersistent, new DocumentTreeCallback() {
    @Override
    public void run(@Nullable DocumentHandle handle) {
        // handle may be null if user cancelled or the request ended in failure
    }
});
```

## Requesting READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
<b> I have not tried uploading apps to the Play Store with the permissions READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE</b><br>
On Android 10 and below, it is possible to request the user permission to use the `File` API (the benefit of this is the use of regular `FileHandle` and massive performance increases).<br>
If you want `ScopedStorageHandler` to handle this, you'll have to set your `minSdkVersion` to `23` (Android 6) and follow the steps below:

First add `implementation 'androidx.activity:activity:1.6.1'` to your Android dependencies and make your `AndroidLauncher` extend `AndroidComponentApplication`<br>

Then you'll have to create a `PermissionsLauncher` class and feed it to `ScopedStorageHandler` when creating it in `onCreate`.<br>
The `AndroidComponentApplication` class is provided by this library.

### Kotlin
```kotlin
class AndroidLauncher : AndroidComponentApplication() {
    private lateinit var scopedHandler: ScopedStorageHandler
    private val permsLauncher = PermissionsLauncher(this) // MUST BE CREATED HERE, NOT INSIDE onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration()
        // ScopedStorageHandler requires a PermissionsLauncher when your application extends AndroidComponentApplication
        scopedHandler = ScopedStorageHandler(this, permsLauncher)
        initialize(ScopedTest(), config)
    }
    
    @Suppress("OVERRIDE_DEPRECATION") // You'll probably want to add this, since onActivityResult is deprecated in ComponentActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scopedHandler.onActivityResult(requestCode, resultCode, data)
    }
}
```

### Java
```java
public class AndroidLauncher extends AndroidComponentApplication {
    private ScopedStorageHandler scopedHandler;
    private final PermissionsLauncher permsLauncher = new PermissionsLauncher(this); // MUST BE CREATED HERE, NOT INSIDE onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // ScopedStorageHandler requires a PermissionsLauncher when your application extends AndroidComponentApplication
        scopedHandler = new ScopedStorageHandler(this, permsLauncher);
        initialize(new ScopedTest(), config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        scopedHandler.onActivityResult(requestCode, resultCode, data);
    }
}
```

Finally, you'll be able to call `scopedHandler.requestReadWritePermissions` which will return a success/failure boolean.<br>
### Kotlin
```kotlin
scopedHandler.requestReadWritePermissions { success ->
    // If success is false, user denied permission or something went wrong
    // Requesting permission multiple times will automatically run this callback
    // with success = true without showing a popup if the user previously granted permission
}
```
### Java
```java
scopedHandler.requestReadWritePermissions(new ReadWriteCallback() {
    @Override
    public void run(boolean success) {
        // If success is false, user denied permission or something went wrong
        // Requesting permission multiple times will automatically run this callback
        // with success = true without showing a popup if the user previously granted permission
    }
});
```

Don't forget to add the permissions to your `AndroidManifest.xml` otherwise this may not work.<br>
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```