package org.wordpress.android.ui.activitylog.list

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_log_list_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailActivity
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_ACTION_STATE_ID
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_REWIND_ID
import org.wordpress.android.ui.jetpack.common.JetpackBackupDownloadActionState
import org.wordpress.android.ui.jetpack.restore.KEY_RESTORE_RESTORE_ID
import org.wordpress.android.ui.jetpack.restore.KEY_RESTORE_REWIND_ID
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.util.config.RestoreFeatureConfig
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWINDABLE_ONLY_KEY
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWIND_ID_KEY
import javax.inject.Inject

class ActivityLogListActivity : LocaleAwareActivity(),
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface {
    @Inject lateinit var restoreFeatureConfig: RestoreFeatureConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.activity_log_list_activity)
        checkAndUpdateUiToBackupScreen()

        setSupportActionBar(toolbar_main)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * It was decided to reuse the 'Activity Log' screen instead of creating a new 'Backup' screen. This was due to the
     * fact that there will be lots of code that would need to be duplicated for the new 'Backup' screen. On the other
     * hand, not much more complexity would be introduced if the 'Activity Log' screen is reused (mainly some 'if/else'
     * code branches here and there).
     *
     * However, should more 'Backup' related additions are added to the 'Activity Log' screen, then it should become a
     * necessity to split those features in separate screens in order not to increase further the complexity of this
     * screen's architecture.
     */
    private fun checkAndUpdateUiToBackupScreen() {
        if (intent.getBooleanExtra(ACTIVITY_LOG_REWINDABLE_ONLY_KEY, false)) {
            setTitle(R.string.backup)
            activity_type_filter.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.ACTIVITY_LOG_DETAIL -> {
                when (data?.getStringExtra(ActivityLogDetailActivity.EXTRA_INNER_FLOW)) {
                    ActivityLogDetailActivity.EXTRA_RESTORE_FLOW -> onActivityResultForRestore(data)
                    ActivityLogDetailActivity.EXTRA_BACKUP_DOWNLOAD_FLOW -> onActivityResultForBackupDownload(data)
                    else -> onActivityResultForActivityLogDetails(data)
                }
            }
            RequestCodes.RESTORE -> onActivityResultForRestore(data)
            RequestCodes.BACKUP_DOWNLOAD -> onActivityResultForBackupDownload(data)
        }
    }

    private fun onActivityResultForActivityLogDetails(data: Intent?) {
        data?.getStringExtra(ACTIVITY_LOG_REWIND_ID_KEY)?.let {
            passRestoreConfirmation(it)
        }
    }

    private fun onActivityResultForRestore(data: Intent?) {
        val rewindId = data?.getStringExtra(KEY_RESTORE_REWIND_ID)
        val restoreId = data?.getLongExtra(KEY_RESTORE_RESTORE_ID, 0)
        if (rewindId != null && restoreId != null) {
            passQueryRestoreStatus(rewindId, restoreId)
        }
    }

    private fun onActivityResultForBackupDownload(data: Intent?) {
        val rewindId = data?.getStringExtra(KEY_BACKUP_DOWNLOAD_REWIND_ID)
        val downloadId = data?.getLongExtra(KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID, 0)
        val actionState = data?.getIntExtra(KEY_BACKUP_DOWNLOAD_ACTION_STATE_ID, 0)
                ?: JetpackBackupDownloadActionState.CANCEL.id
        if (actionState != JetpackBackupDownloadActionState.CANCEL.id && rewindId != null && downloadId != null) {
            passQueryBackupDownloadStatus(rewindId, downloadId, actionState)
        }
    }

    override fun onPositiveClicked(instanceTag: String) {
        passRestoreConfirmation(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        // Unused
    }

    private fun passRestoreConfirmation(rewindId: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onRestoreConfirmed(rewindId)
        }
    }

    private fun passQueryRestoreStatus(rewindId: String, restoreId: Long) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onQueryRestoreStatus(rewindId, restoreId)
        }
    }

    private fun passQueryBackupDownloadStatus(rewindId: String, downloadId: Long, actionState: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onQueryBackupDownloadStatus(rewindId, downloadId, actionState)
        }
    }
}
