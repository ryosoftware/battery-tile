package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

class BatteryOverlay(private val context: Context, private val prefs: BatteryOverlayPreferences) {
    private open inner class BatteryOverlayView(context: Context): View(context) {
        protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var pulsing = false
        private var pulseProgress = 0f
        private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                pulseProgress = it.animatedValue as Float
                invalidate()
            }
        }

        private fun getColor(): Color =
            getColor(level, isCharging, context, prefs)

        protected fun setPaintColor() {
            val color = getColor()
            paint.color = color.toArgb()

            if (pulsing) {
                val alphaFraction = (MIN_PULSE_ALPHA + (1f - MIN_PULSE_ALPHA) * pulseProgress)
                paint.alpha = (color.alpha * alphaFraction * 255f).roundToInt()
            } else {
                paint.alpha = 255
            }
        }

        fun setPulsing(enabled: Boolean) {
            if (pulsing == enabled) return

            pulsing = enabled

            if (enabled) {
                pulseProgress = 0f
                pulseAnimator.start()
            } else {
                pulseAnimator.cancel()
                pulseProgress = 0f
            }

            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawColor(
                android.graphics.Color.TRANSPARENT,
                android.graphics.PorterDuff.Mode.CLEAR
            )
        }

    }
    private inner class BatteryBarOverlayView(context: Context) : BatteryOverlayView(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            setPaintColor()

            val fillWidth = width * (level / 100f)

            canvas.drawRect(
                0f,
                0f,
                fillWidth,
                height.toFloat(),
                paint
            )
        }
    }

    private inner class BatteryRingOverlayView(context: Context, radiusPx: Float, strokeWidthPx: Float, viewSizePx: Int) : BatteryOverlayView(context) {
        private val ringBounds = RectF()

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidthPx

            val center = viewSizePx / 2f
            ringBounds.set(
                center - radiusPx,
                center - radiusPx,
                center + radiusPx,
                center + radiusPx,
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            setPaintColor()

            val sweepAngle = 360f * (level / 100f)
            val startAngle = 270f - sweepAngle.coerceIn(0f, 360f)

            canvas.drawArc(
                ringBounds,
                startAngle,
                sweepAngle.coerceIn(0f, 360f),
                false,
                paint,
            )
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        setOverlayAttachStatus()
        update()
    }

    private val eventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (prefs.batteryOverlayEnabled) {
                        Main.from(this@BatteryOverlay.context).batteryIntentProvider.get(true)
                            ?.let { update(BatteryIntentHelper(this@BatteryOverlay.context, it, null)) }
                    }
                    refreshPulse()
                }

                Intent.ACTION_SCREEN_OFF -> refreshPulse()

                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (prefs.batteryOverlayEnabled && isScreenOn()) {
                        Main.from(this@BatteryOverlay.context).batteryIntentProvider.get(true)
                            ?.let { update(BatteryIntentHelper(this@BatteryOverlay.context, it, null)) }
                    }
                }

                Intent.ACTION_BATTERY_CHANGED -> {
                    if (prefs.batteryOverlayEnabled && isScreenOn()) {
                        update(BatteryIntentHelper(this@BatteryOverlay.context, intent, null))
                    }
                }
            }
        }
    }

    private var level = 0
    private var isCharging = false

    private var overlayView: BatteryOverlayView? = null

    private var lastAttachedMode: BatteryOverlayMode? = null
    private var barHeightPx: Int? = null
    private var ringRadiusPx: Int? = null
    private var ringThicknessPx: Int? = null
    private var ringOffsetXPx: Int? = null
    private var ringOffsetYPx: Int? = null
    private val windowManager by lazy { context.getSystemService(WINDOW_SERVICE) as WindowManager }

    init {
        setOverlayAttachStatus()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(eventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(eventsReceiver, filter)
        }

        prefs.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    fun dispose() {
        detachOverlay()
        context.unregisterReceiver(eventsReceiver)
        prefs.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun isScreenOn(): Boolean =
        context.getSystemService(PowerManager::class.java).isInteractive

    private fun shouldPulse(): Boolean =
        isCharging && isScreenOn()

    private fun refreshPulse() {
        overlayView?.setPulsing(shouldPulse())
    }

    private fun attachOverlay() {
        if (prefs.batteryOverlayEnabled && (overlayView == null)) {
            val mode = prefs.overlayMode
            val density = context.resources.displayMetrics.density

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START

            if (mode == BatteryOverlayMode.RING) {
                val halfSize = prefs.ringRadiusPx + prefs.ringThicknessPx + RING_PADDING_DP * density
                val size = (halfSize * 2f).toInt()

                params.width = size
                params.height = size

                params.x = (prefs.ringOffsetXPx - halfSize).roundToInt()
                params.y = (prefs.ringOffsetYPx - halfSize).roundToInt()

                runCatching {
                    val view = BatteryRingOverlayView(context, radiusPx = prefs.ringRadiusPx.toFloat(), strokeWidthPx = prefs.ringThicknessPx.toFloat(), viewSizePx = size)

                    windowManager.addView(view, params)

                    overlayView = view
                    lastAttachedMode = mode
                    ringRadiusPx = prefs.ringRadiusPx
                    ringThicknessPx = prefs.ringThicknessPx
                    ringOffsetXPx = prefs.ringOffsetXPx
                    ringOffsetYPx = prefs.ringOffsetYPx
                }
            } else {
                params.height = prefs.barHeightPx

                runCatching {
                    val view = BatteryBarOverlayView(context)

                    windowManager.addView(view, params)

                    overlayView = view
                    lastAttachedMode = mode
                    barHeightPx = prefs.barHeightPx
                }
            }
        }

        refreshPulse()
    }

    private fun detachOverlay() {
        overlayView?.let {
            overlayView?.setPulsing(false)
            try { windowManager.removeView(it) }
            catch (_: Exception) { }
            finally {
                overlayView = null
                lastAttachedMode = null
                barHeightPx = null
                ringRadiusPx = null
                ringThicknessPx = null
                ringOffsetXPx = null
                ringOffsetYPx = null
            }
        }
    }

    fun setOverlayAttachStatus() {
        val batteryOverlayEnabled = prefs.batteryOverlayEnabled
        val mode = prefs.overlayMode

        val modeChanged = lastAttachedMode != mode
        val barHeightChanged = (barHeightPx != null) && (barHeightPx != prefs.barHeightPx)
        val ringParamsChanged = (mode == BatteryOverlayMode.RING) && ((ringRadiusPx != prefs.ringRadiusPx) || (ringThicknessPx != prefs.ringThicknessPx) || (ringOffsetXPx != prefs.ringOffsetXPx) || (ringOffsetYPx != prefs.ringOffsetYPx))

        if (!batteryOverlayEnabled || modeChanged || barHeightChanged || ringParamsChanged) {
            detachOverlay()
        }

        if (batteryOverlayEnabled) {
            if (overlayView == null) { attachOverlay() }

            val batteryIntent = Main.from(context).batteryIntentProvider.get(true)
            val batteryIntentHelper = batteryIntent?.let { BatteryIntentHelper(context, it, null) }
            if (batteryIntentHelper != null) update(batteryIntentHelper)
        }
    }

    private fun update() =
        overlayView?.invalidate()

    fun update(batteryIntentHelper: BatteryIntentHelper) {
        level = batteryIntentHelper.level
        isCharging = batteryIntentHelper.isCharging

        refreshPulse()
        update()
    }

    companion object {
        private const val RING_PADDING_DP = 8f
        private const val PULSE_DURATION_MS = 1000L
        private const val MIN_PULSE_ALPHA = 0.25f

        fun getColor(level: Int, isCharging: Boolean, context: Context, prefs: BatteryOverlayPreferences): Color {
            if (isCharging) return prefs.chargingColor

            val count = prefs.segmentsCount
            for (index in 0 until count) {
                if (level >= prefs.getSegmentStartLevel(index)) return prefs.getSegmentColor(index)
            }

            return Color(context.getColor(R.color.battery_critical))
        }
    }
}