package kurante.gdxscopedstorage.launchers

import android.content.Context
import android.content.Intent

interface ActivityLauncher {
    fun startActivityForResult(intent: Intent, requestCode: Int)
    val context: Context
}