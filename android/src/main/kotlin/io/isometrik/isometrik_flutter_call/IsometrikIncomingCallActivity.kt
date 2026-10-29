package io.isometrik.isometrik_flutter_call

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Native incoming-call screen. Notification body tap and the lock-screen
 * full-screen intent open this activity. Accept and Reject are the only
 * actions that answer or decline.
 */
class IsometrikIncomingCallActivity : Activity() {
    private var callId: String = ""
    private var viewsReady: Boolean = false
    private var actionTaken: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLockScreenFlags()
        setContentView(R.layout.activity_incoming_call)
        viewsReady = true
        showingRef = WeakReference(this)

        findViewById<Button>(R.id.incoming_reject_button).setOnClickListener {
            rejectCall()
        }
        findViewById<Button>(R.id.incoming_accept_button).setOnClickListener {
            acceptCall()
        }
        bindFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        actionTaken = false
        bindFromIntent(intent)
    }

    override fun onDestroy() {
        if (showingRef?.get() === this) {
            showingRef = null
        }
        super.onDestroy()
    }

    private fun applyLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun bindFromIntent(source: Intent?) {
        val resolvedCallId =
            source?.getStringExtra(IsometrikFlutterCallPlugin.EXTRA_INCOMING_CALL_ID)
                ?.trim()
                .orEmpty()
                .ifEmpty { source?.getStringExtra("callId")?.trim().orEmpty() }
        val callerName = source?.getStringExtra(EXTRA_CALLER_NAME)?.trim().orEmpty()
        val hasVideo = source?.getBooleanExtra(EXTRA_HAS_VIDEO, false) ?: false
        bindIncoming(resolvedCallId, callerName, hasVideo)
    }

    private fun bindIncoming(resolvedCallId: String, callerName: String, hasVideo: Boolean) {
        callId = resolvedCallId
        if (!viewsReady) return
        findViewById<TextView>(R.id.incoming_caller_name).text =
            callerName.ifBlank { "Isometrik Call" }
        findViewById<TextView>(R.id.incoming_call_label).setText(
            if (hasVideo) {
                R.string.isometrik_incoming_video_call_label
            } else {
                R.string.isometrik_incoming_call_label
            },
        )
    }

    private fun acceptCall() {
        if (actionTaken) return
        actionTaken = true
        Log.d(TAG, "Accept ringing UI callId=$callId")
        if (callId.isNotEmpty()) {
            IsometrikFlutterCallPlugin.notifyIncomingCallAcceptedFromLaunch(
                applicationContext,
                callId,
            )
        }
        requestDismissKeyguard()
        IsometrikFlutterCallPlugin.launchHostMainActivityForIncomingCall(this, callId)
        finish()
    }

    private fun rejectCall() {
        if (actionTaken) return
        actionTaken = true
        Log.d(TAG, "Reject ringing UI callId=$callId")
        if (callId.isNotEmpty()) {
            IsometrikFlutterCallPlugin.notifyIncomingCallRejectedFromLaunch(
                applicationContext,
                callId,
            )
        }
        finish()
    }

    private fun requestDismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    companion object {
        private const val TAG = "IsometrikFlutterCall"
        private const val EXTRA_CALLER_NAME = "callerName"
        private const val EXTRA_HAS_VIDEO = "hasVideo"

        private val mainHandler = Handler(Looper.getMainLooper())
        private var pendingFinish: Runnable? = null
        private var showingRef: WeakReference<IsometrikIncomingCallActivity>? = null

        fun ringingIntent(
            context: Context,
            callId: String,
            callerName: String,
            hasVideo: Boolean,
        ): Intent {
            val activity = showingRef?.get()?.takeIf { !it.isFinishing }
            activity?.runOnUiThread {
                activity.bindIncoming(callId, callerName, hasVideo)
            }
            return Intent(context, IsometrikIncomingCallActivity::class.java).apply {
                putExtra(IsometrikFlutterCallPlugin.EXTRA_INCOMING_CALL_ID, callId)
                putExtra("callId", callId)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_HAS_VIDEO, hasVideo)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            }
        }

        /**
         * Cancels a finish posted by [finishIfShowing].
         * [startIncomingRingtone] dismisses alerts before the new notification is posted;
         * the following [keepShowing] keeps this screen up for the new call.
         */
        fun keepShowing() {
            pendingFinish?.let { mainHandler.removeCallbacks(it) }
            pendingFinish = null
        }

        fun finishIfShowing() {
            val finish =
                Runnable {
                    pendingFinish = null
                    showingRef?.get()?.let { activity ->
                        if (!activity.isFinishing) activity.finish()
                    }
                }
            pendingFinish?.let { mainHandler.removeCallbacks(it) }
            pendingFinish = finish
            mainHandler.post(finish)
        }
    }
}
