package info.dvkr.screenstream.ui.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.R
import info.dvkr.screenstream.data.other.getLog
import info.dvkr.screenstream.data.settings.SettingsReadOnly
import info.dvkr.screenstream.service.helper.IntentAction
import org.koin.android.ext.android.inject


class PermissionActivity : AppCompatActivity() {

    companion object {
        fun getStartIntent(context: Context): Intent =
            Intent(context.applicationContext, PermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val requestCodeScreenCapture = 10
    private val settingsReadOnly: SettingsReadOnly by inject()

    private var materialDialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        setNightMode(settingsReadOnly.nightMode)
        super.onCreate(savedInstanceState)

        if (materialDialog != null)
            XLog.e(getLog("onCreate", "materialDialog != null"), IllegalStateException("materialDialog != null"))

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), requestCodeScreenCapture)
        } catch (ex: ActivityNotFoundException) {
            showErrorDialog(
                R.string.permission_activity_error_title_activity_not_found,
                R.string.permission_activity_error_activity_not_found
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        XLog.d(getLog("onActivityResult", "requestCode: $requestCode"))

        if (requestCode != requestCodeScreenCapture) {
            XLog.e(getLog("onActivityResult"), IllegalStateException("Unknown requestCode: $requestCode"))
            showErrorDialog()
            return
        }

        if (Activity.RESULT_OK != resultCode) {
            XLog.w(getLog("onActivityResult", "Cast permission denied"))
            showErrorDialog(
                R.string.permission_activity_cast_permission_required_title,
                R.string.permission_activity_cast_permission_required
            )
            return
        }

        if (data == null) {
            XLog.e(getLog("onActivityResult"), IllegalStateException("onActivityResult: data = null"))
            showErrorDialog()
            return
        }

        XLog.d(getLog("onActivityResult", "Cast permission granted"))
        closeActivity(IntentAction.CastIntent(data))
    }

    private fun showErrorDialog(
        @StringRes titleRes: Int = R.string.permission_activity_error_title,
        @StringRes messageRes: Int = R.string.permission_activity_error_unknown
    ) {
        materialDialog?.dismiss()

        materialDialog = MaterialDialog(this).show {
            icon(R.drawable.ic_permission_dialog_24dp)
            title(titleRes)
            message(messageRes)
            cancelable(false)
            cancelOnTouchOutside(false)
            positiveButton(android.R.string.ok) { closeActivity(IntentAction.CastPermissionsDenied) }
        }
    }

    private fun closeActivity(intentAction: IntentAction) {
        materialDialog?.dismiss()
        materialDialog = null
        intentAction.sendToAppService(this@PermissionActivity)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun setNightMode(@AppCompatDelegate.NightMode nightMode: Int) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
        delegate.setLocalNightMode(nightMode)
    }
}