package kurante.gdxscopedstorage.launchers

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidComponentApplication
import kurante.gdxscopedstorage.util.PermissionsLauncher

@Suppress("DEPRECATION")
class ComponentLauncher(
    private val application: AndroidComponentApplication,
    val permissionLauncher: PermissionsLauncher,
) : ActivityLauncher {
    override val context: Context
        get() = application

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        application.startActivityForResult(intent, requestCode)
    }
}

interface ReadWriteCallback {
    fun run(success: Boolean)
}