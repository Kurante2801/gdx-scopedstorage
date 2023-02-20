package kurante.gdxscopedstorage.request

import android.content.Intent

interface ActivityRequest {
    val requestCode: Int
    fun onResult(resultCode: Int, data: Intent?)
}