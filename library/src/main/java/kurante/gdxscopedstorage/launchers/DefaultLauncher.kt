package kurante.gdxscopedstorage.launchers

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

// The default libGDX AndroidApplication
class DefaultLauncher(
    private val application: Activity,
) : ActivityLauncher {
    override val context: Context
        get() = application

    override fun startActivityForResult(intent: Intent, requestCode: Int) =
        application.startActivityForResult(intent, requestCode)
}