package com.simplekioskbrowser.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.simplekioskbrowser.R

/**
 * A thin left-edge strip that detects a deliberate inward (left-to-right) swipe and reports it
 * through [setOnEdgeSwipeListener].
 *
 * Design notes (see FR-05 / hidden gesture):
 *  - The strip is narrow ([R.dimen.edge_strip_width]) so it only reserves the extreme screen
 *    edge for the gesture, limiting interference with page scrolling.
 *  - A gesture only fires for a clear horizontal swipe whose horizontal movement dominates the
 *    vertical movement and exceeds a distance threshold. Slow vertical drags starting in the
 *    strip are not treated as the gesture.
 *  - Because the strip captures touch within its bounds, a vertical scroll begun exactly at the
 *    screen edge may be intercepted. This is an accepted, documented trade-off for a hidden
 *    edge gesture; ordinary page scrolling from anywhere else on the page is unaffected.
 */
class EdgeSwipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onEdgeSwipe: (() -> Unit)? = null

    private val edgeWidthPx: Int
    private val touchSlopPx: Float
    private val minSwipeDxPx: Float

    private var startX: Float = -1f
    private var startY: Float = -1f
    private var tracking = false

    init {
        val configuration = ViewConfiguration.get(context)
        touchSlopPx = configuration.scaledTouchSlop.toFloat()
        // The swipe must move at least half the strip width inward and clearly exceed touch slop.
        edgeWidthPx = resources.getDimensionPixelSize(R.dimen.edge_strip_width)
        minSwipeDxPx = Math.max(edgeWidthPx * 0.5f, touchSlopPx * 2f)
    }

    fun setOnEdgeSwipeListener(listener: () -> Unit) {
        onEdgeSwipe = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < edgeWidthPx.toFloat()) {
                    startX = event.x
                    startY = event.y
                    tracking = true
                    return true // capture the pointer while we evaluate the gesture
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return false
                val dx = event.x - startX
                val dyAbs = Math.abs(event.y - startY)
                // Clear inward horizontal swipe: horizontal movement dominates and passes threshold.
                if (dx > minSwipeDxPx && dx > dyAbs * 1.5f) {
                    tracking = false
                    onEdgeSwipe?.invoke()
                    return true
                }
                // Still evaluating: retain capture so a quick horizontal swipe isn't lost.
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracking = false
                startX = -1f
                startY = -1f
                return false
            }
        }
        return super.onTouchEvent(event)
    }
}
