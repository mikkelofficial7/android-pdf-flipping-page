package com.lib.pdfflipbook.effect

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.lib.flipbookengine.R
import com.lib.pdfflipbook.listener.PageRunningListener
import java.lang.ref.WeakReference

class PaperFoldView : View {
    private lateinit var pageRunningListener: PageRunningListener
    private var mTextPaint: Paint? = null
    private var mTextPaintShadow: TextPaint? = null

    /** Px / Draw call  */
    private var mCurlSpeed = 0

    /** Fixed update time used to create a smooth curl animation  */
    private var mUpdateRate = 0

    /** The initial offset for x and y axis movements  */
    private var mInitialEdgeOffset = 0

    /** The mode we will use  */
    private var mCurlMode = 0

    /** Enable/Disable debug mode  */
    private var bEnableDebugMode = false

    /** The context which owns us  */
    private var mContext: WeakReference<Context>? = null

    /** Handler used to auto flip time based  */
    private var mAnimationHandler: FlipAnimationHandler? = null

    /** Maximum radius a page can be flipped, by default it's the width of the view  */
    private var mFlipRadius = 0f

    /** Point used to move  */
    private var mMovement: Vector2D? = null

    /** The finger position  */
    private var mFinger: Vector2D? = null

    /** Movement point form the last frame  */
    private var mOldMovement: Vector2D? = null

    /** Page curl edge  */
    private var mCurlEdgePaint: Paint? = null

    /** Actual proportional content size  */
    private var actualContentHeight: Int = 0
    private var actualContentWidth: Int = 0

    /** Our points used to define the current clipping paths in our draw call  */
    private var mA: Vector2D? = null
    private var mB: Vector2D? = null
    private var mC: Vector2D? = null
    private var mD: Vector2D? = null
    private var mE: Vector2D? = null
    private var mF: Vector2D? = null
    private var mOldF: Vector2D? = null
    private var mOrigin: Vector2D? = null

    /** Left and top offset to be applied when drawing  */
    private var mCurrentLeft = 0
    private var mCurrentTop = 0

    /** If false no draw call has been done  */
    private var bViewDrawn = false

    /** Defines the flip direction that is currently considered  */
    private var bFlipRight = false

    /** If TRUE we are currently auto-flipping  */
    private var bFlipping = false

    /** TRUE if the user moves the pages  */
    private var bUserMoves = false

    /** Used to control touch input blocking  */
    private var bBlockTouchInput = false

    /** Enable input after the next draw event  */
    private var bEnableInputAfterDraw = false

    /** LAGACY The current foreground  */
    private var mForeground: Bitmap? = null

    /** LAGACY The current background  */
    private var mBackground: Bitmap? = null

    /** LAGACY List of pages, this is just temporal  */
    private var mPages: ArrayList<Bitmap> = arrayListOf()

    /** LAGACY Current selected page  */
    private var mIndex = 0

    /**
     * Inner class used to represent a 2D point.
     */
    private inner class Vector2D(var x: Float, var y: Float) {
        override fun toString(): String {
            return "(${this.x},$y)"
        }

        fun length(): Float {
            return Math.sqrt((x * x + y * y).toDouble()).toFloat()
        }

        fun lengthSquared(): Float {
            return x * x + y * y
        }

        override fun equals(other: Any?): Boolean {
            if (other is Vector2D) {
                val p = other
                return p.x == x && p.y == y
            }
            return false
        }

        fun reverse(): Vector2D {
            return Vector2D(-x, -y)
        }

        fun sum(b: Vector2D): Vector2D {
            return Vector2D(x + b.x, y + b.y)
        }

        fun sub(b: Vector2D?): Vector2D {
            return Vector2D(x - b!!.x, y - b.y)
        }

        fun dot(vec: Vector2D): Float {
            return x * vec.x + y * vec.y
        }

        fun cross(a: Vector2D, b: Vector2D): Float {
            return a.cross(b)
        }

        fun cross(vec: Vector2D): Float {
            return x * vec.y - y * vec.x
        }

        fun distanceSquared(other: Vector2D?): Float {
            val dx = other!!.x - x
            val dy = other.y - y
            return dx * dx + dy * dy
        }

        fun distance(other: Vector2D?): Float {
            return Math.sqrt(distanceSquared(other).toDouble()).toFloat()
        }

        fun dotProduct(other: Vector2D): Float {
            return other.x * x + other.y * y
        }

        fun normalize(): Vector2D {
            val magnitude = Math.sqrt(dotProduct(this).toDouble()).toFloat()
            return Vector2D(x / magnitude, y / magnitude)
        }

        fun mult(scalar: Float): Vector2D {
            return Vector2D(x * scalar, y * scalar)
        }
    }

    /**
     * Inner class used to make a fixed timed animation of the curl effect.
     */
    @SuppressLint("HandlerLeak")
    internal inner class FlipAnimationHandler : Handler() {
        override fun handleMessage(msg: Message) {
            FlipAnimationStep()
        }

        fun sleep(millis: Long) {
            this.removeMessages(0)
            sendMessageDelayed(obtainMessage(0), millis)
        }
    }

    /**
     * Base
     * @param context
     */
    constructor(context: Context) : super(context) {
        init(context)
        ResetClipEdge()
    }

    /**
     * Construct the object from an XML file. Valid Attributes:
     *
     * @see android.view.View.View
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)

        // Get the data from the XML AttributeSet
        run {
            val a =
                context.obtainStyledAttributes(attrs, R.styleable.PageCurlView)

            // Get data
            bEnableDebugMode =
                a.getBoolean(R.styleable.PageCurlView_enableDebugMode, bEnableDebugMode)
            mCurlSpeed = a.getInt(R.styleable.PageCurlView_curlSpeed, mCurlSpeed)
            mUpdateRate = a.getInt(R.styleable.PageCurlView_updateRate, mUpdateRate)
            mInitialEdgeOffset =
                a.getInt(R.styleable.PageCurlView_initialEdgeOffset, mInitialEdgeOffset)
            mCurlMode = a.getInt(R.styleable.PageCurlView_curlMode, mCurlMode)
            Log.i(TAG, "mCurlSpeed: $mCurlSpeed")
            Log.i(TAG, "mUpdateRate: $mUpdateRate")
            Log.i(
                TAG,
                "mInitialEdgeOffset: $mInitialEdgeOffset"
            )
            Log.i(TAG, "mCurlMode: $mCurlMode")

            // recycle object (so it can be used by others)
            a.recycle()
        }
        ResetClipEdge()
    }

    /**
     * Initialize the view
     */
    fun setData(data: List<Bitmap>, listener: PageRunningListener) {
        mPages = ArrayList()
        mPages.clear()
        mPages.addAll(data)

        pageRunningListener = listener

        mForeground = if (mPages.size > 0) mPages.first() else null
        mBackground = if (mPages.size > 1) mPages[1] else null

        pageRunningListener.onCurrentPageRunning(mForeground)
        init(context)
    }

    fun setActualContentSize(height: Int, width: Int) {
        actualContentHeight = height
        actualContentWidth = width
    }

    private fun init(context: Context) {
        // Foreground text paint
        mTextPaint = Paint()
        mTextPaint!!.isAntiAlias = true
        mTextPaint!!.textSize = 16f
        mTextPaint!!.color = context.getColor(R.color.black)

        // The shadow
        mTextPaintShadow = TextPaint()
        mTextPaintShadow!!.isAntiAlias = true
        mTextPaintShadow!!.textSize = 16f
        mTextPaintShadow!!.color = context.getColor(R.color.black)

        // Cache the context
        mContext = WeakReference(context)

        // Base padding
        setPadding(3, 3, 3, 3)

        // The focus flags are needed
        isFocusable = true
        isFocusableInTouchMode = true
        mMovement = Vector2D(0f, 0f)
        mFinger = Vector2D(0f, 0f)
        mOldMovement = Vector2D(0f, 0f)

        // Create our curl animation handler
        mAnimationHandler = FlipAnimationHandler()

        // Create our edge paint
        mCurlEdgePaint = Paint()
        mCurlEdgePaint!!.colorFilter = PorterDuffColorFilter(context.getColor(R.color.gray), PorterDuff.Mode.SRC_ATOP)
        mCurlEdgePaint!!.isAntiAlias = true
        mCurlEdgePaint!!.style = Paint.Style.FILL
        mCurlEdgePaint!!.setShadowLayer(10f, -5f, 5f, -0x67000000)

        // Set the default props, those come from an XML :D
        mCurlSpeed = 200
        mUpdateRate = 33
        mInitialEdgeOffset = 20
        mCurlMode = 1
    }

    /**
     * Reset points to it's initial clip edge state
     */
    fun ResetClipEdge() {
        // Set our base movement
        mMovement!!.x = mInitialEdgeOffset.toFloat()
        mMovement!!.y = mInitialEdgeOffset.toFloat()
        mOldMovement!!.x = 0f
        mOldMovement!!.y = 0f

        // Now set the points
        // TODO: OK, those points MUST come from our measures and
        // the actual bounds of the view!
        mA = Vector2D(mInitialEdgeOffset.toFloat(), 0f)
        mB = Vector2D(this.width.toFloat(), this.height.toFloat())
        mC = Vector2D(this.width.toFloat(), 0f)
        mD = Vector2D(0f, 0f)
        mE = Vector2D(0f, 0f)
        mF = Vector2D(0f, 0f)
        mOldF = Vector2D(0f, 0f)

        // The movement origin point
        mOrigin = Vector2D(this.width.toFloat(), 0f)
    }

    /**
     * Return the context which created use. Can return null if the
     * context has been erased.
     */
    private fun GetContext(): Context? {
        return mContext!!.get()
    }

    /**
     * See if the current curl mode is dynamic
     * @return TRUE if the mode is CURLMODE_DYNAMIC, FALSE otherwise
     */
    fun IsCurlModeDynamic(): Boolean {
        return mCurlMode == CURLMODE_DYNAMIC
    }

    /**
     * Set the curl speed.
     * @param curlSpeed - New speed in px/frame
     * @throws IllegalArgumentException if curlspeed < 1
     */
    fun SetCurlSpeed(curlSpeed: Int) {
        require(curlSpeed >= 1) { "curlSpeed must be greated than 0" }
        mCurlSpeed = curlSpeed
    }

    /**
     * Get the current curl speed
     * @return int - Curl speed in px/frame
     */
    fun GetCurlSpeed(): Int {
        return mCurlSpeed
    }

    /**
     * Set the update rate for the curl animation
     * @param updateRate - Fixed animation update rate in fps
     * @throws IllegalArgumentException if updateRate < 1
     */
    fun SetUpdateRate(updateRate: Int) {
        require(updateRate >= 1) { "updateRate must be greated than 0" }
        mUpdateRate = updateRate
    }

    /**
     * Get the current animation update rate
     * @return int - Fixed animation update rate in fps
     */
    fun GetUpdateRate(): Int {
        return mUpdateRate
    }

    /**
     * Set the initial pixel offset for the curl edge
     * @param initialEdgeOffset - px offset for curl edge
     * @throws IllegalArgumentException if initialEdgeOffset < 0
     */
    fun SetInitialEdgeOffset(initialEdgeOffset: Int) {
        require(initialEdgeOffset >= 0) { "initialEdgeOffset can not negative" }
        mInitialEdgeOffset = initialEdgeOffset
    }

    /**
     * Get the initial pixel offset for the curl edge
     * @return int - px
     */
    fun GetInitialEdgeOffset(): Int {
        return mInitialEdgeOffset
    }

    /**
     * Set the curl mode.
     *
     * Can be one of the following values:
     * <table>
     * <colgroup align="left"></colgroup>
     * <colgroup align="left"></colgroup>
     * <tr><th>Value</th><th>Description</th></tr>
     * <tr><td>`[com.dcg.pagecurl:CURLMODE_SIMPLE][.CURLMODE_SIMPLE]`</td><td>Curl target will move only in one axis.</td></tr>
     * <tr><td>`[com.dcg.pagecurl:CURLMODE_DYNAMIC][.CURLMODE_DYNAMIC]`</td><td>Curl target will move on both X and Y axis.</td></tr>
    </table> *
     * @see .CURLMODE_SIMPLE
     *
     * @see .CURLMODE_DYNAMIC
     *
     * @param curlMode
     * @throws IllegalArgumentException if curlMode is invalid
     */
    fun SetCurlMode(curlMode: Int) {
        require(
            !(curlMode != CURLMODE_SIMPLE &&
                    curlMode != CURLMODE_DYNAMIC)
        ) { "Invalid curlMode" }
        mCurlMode = curlMode
    }

    fun GetCurlMode(): Int {
        return mCurlMode
    }

    /**
     * Enable debug mode. This will draw a lot of data in the view so you can track what is happening
     * @param bFlag - boolean flag
     */
    fun SetEnableDebugMode(bFlag: Boolean) {
        bEnableDebugMode = bFlag
    }

    /**
     * Check if we are currently in debug mode.
     * @return boolean - If TRUE debug mode is on, FALSE otherwise.
     */
    fun IsDebugModeEnabled(): Boolean {
        return bEnableDebugMode
    }

    /**
     * @see android.view.View.measure
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val finalWidth: Int
        val finalHeight: Int
        finalWidth = measureWidth(widthMeasureSpec)
        finalHeight = measureHeight(heightMeasureSpec)
        setMeasuredDimension(finalWidth, finalHeight)
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private fun measureWidth(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        result = if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            specSize
        } else {
            // Measure the text
            specSize
        }
        return result
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private fun measureHeight(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        result = if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            specSize
        } else {
            // Measure the text (beware: ascent is a negative number)
            specSize
        }
        return result
    }

    /**
     * Render the text
     *
     * @see android.view.View.onDraw
     */
    //---------------------------------------------------------------
    // Curling. This handles touch events, the actual curling
    // implementations and so on.
    //---------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!bBlockTouchInput) {

            // Get our finger position
            mFinger!!.x = event.x
            mFinger!!.y = event.y
            val width = width
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mOldMovement!!.x = mFinger!!.x
                    mOldMovement!!.y = mFinger!!.y

                    // If we moved over the half of the display flip to next
                    if (mOldMovement!!.x > width shr 1) {
                        mMovement!!.x = mInitialEdgeOffset.toFloat()
                        mMovement!!.y = mInitialEdgeOffset.toFloat()

                        // Set the right movement flag
                        bFlipRight = true
                    } else {
                        // Set the left movement flag
                        bFlipRight = false

                        // go to next previous page
                        previousView()

                        // Set new movement
                        mMovement!!.x = (if (IsCurlModeDynamic()) width shl 1 else width).toFloat()
                        mMovement!!.y = mInitialEdgeOffset.toFloat()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    bUserMoves = false
                    bFlipping = true
                    FlipAnimationStep()
                }

                MotionEvent.ACTION_MOVE -> {
                    bUserMoves = true

                    // Get movement
                    mMovement!!.x -= mFinger!!.x - mOldMovement!!.x
                    mMovement!!.y -= mFinger!!.y - mOldMovement!!.y
                    mMovement = CapMovement(mMovement, true)

                    // Make sure the y value get's locked at a nice level
                    if (mMovement!!.y <= 1) mMovement!!.y = 1f

                    // Get movement direction
                    bFlipRight = mFinger!!.x < mOldMovement!!.x

                    // Save old movement values
                    mOldMovement!!.x = mFinger!!.x
                    mOldMovement!!.y = mFinger!!.y

                    // Force a new draw call
                    DoPageCurl()
                    this.invalidate()
                }
            }
        }

        // TODO: Only consume event if we need to.
        return true
    }

    /**
     * Make sure we never move too much, and make sure that if we
     * move too much to add a displacement so that the movement will
     * be still in our radius.
     * @param radius - radius form the flip origin
     * @param bMaintainMoveDir - Cap movement but do not change the
     * current movement direction
     * @return Corrected point
     */
    private fun CapMovement(points: Vector2D?, bMaintainMoveDir: Boolean): Vector2D? {
        // Make sure we never ever move too much
        var point = points
        if (point!!.distance(mOrigin) > mFlipRadius) {
            if (bMaintainMoveDir) {
                // Maintain the direction
                point = mOrigin!!.sum(point.sub(mOrigin).normalize().mult(mFlipRadius))
            } else {
                // Change direction
                if (point.x > mOrigin!!.x + mFlipRadius) point.x =
                    mOrigin!!.x + mFlipRadius else if (point.x < mOrigin!!.x - mFlipRadius) point.x =
                    mOrigin!!.x - mFlipRadius
                point.y =
                    (Math.sin(Math.acos((Math.abs(point.x - mOrigin!!.x) / mFlipRadius).toDouble())) * mFlipRadius).toFloat()
            }
        }
        return point
    }

    /**
     * Execute a step of the flip animation
     */
    fun FlipAnimationStep() {
        if (!bFlipping) return
        val width = width

        // No input when flipping
        bBlockTouchInput = true

        // Handle speed
        var curlSpeed = mCurlSpeed.toFloat()
        if (!bFlipRight) curlSpeed *= -1f

        // Move us
        mMovement!!.x += curlSpeed
        mMovement = CapMovement(mMovement, false)

        // Create values
        DoPageCurl()

        // Check for endings :D
        if (mA!!.x < 1 || mA!!.x > width - 1) {
            bFlipping = false
            if (bFlipRight) {
                //SwapViews();
                nextView()
            }
            ResetClipEdge()

            // Create values
            DoPageCurl()

            // Enable touch input after the next draw event
            bEnableInputAfterDraw = true
        } else {
            mAnimationHandler!!.sleep(mUpdateRate.toLong())
        }

        // Force a new draw call
        this.invalidate()
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    private fun DoPageCurl() {
        if (bFlipping) {
            if (IsCurlModeDynamic()) doDynamicCurl() else doSimpleCurl()
        } else {
            if (IsCurlModeDynamic()) doDynamicCurl() else doSimpleCurl()
        }
    }

    /**
     * Do a simple page curl effect
     */
    private fun doSimpleCurl() {
        val width = width
        val height = height

        // Calculate point A
        mA!!.x = width - mMovement!!.x
        mA!!.y = height.toFloat()

        // Calculate point D
        mD!!.x = 0f
        mD!!.y = 0f
        if (mA!!.x > width / 2) {
            mD!!.x = width.toFloat()
            mD!!.y = height - (width - mA!!.x) * height / mA!!.x
        } else {
            mD!!.x = 2 * mA!!.x
            mD!!.y = 0f
        }

        // Now calculate E and F taking into account that the line
        // AD is perpendicular to FB and EC. B and C are fixed points.
        val angle = Math.atan(((height - mD!!.y) / (mD!!.x + mMovement!!.x - width)).toDouble())
        val _cos = Math.cos(2 * angle)
        val _sin = Math.sin(2 * angle)

        // And get F
        mF!!.x = (width - mMovement!!.x + _cos * mMovement!!.x).toFloat()
        mF!!.y = (height - _sin * mMovement!!.x).toFloat()

        // If the x position of A is above half of the page we are still not
        // folding the upper-right edge and so E and D are equal.
        if (mA!!.x > width / 2) {
            mE!!.x = mD!!.x
            mE!!.y = mD!!.y
        } else {
            // So get E
            mE!!.x = (mD!!.x + _cos * (width - mD!!.x)).toFloat()
            mE!!.y = -(_sin * (width - mD!!.x)).toFloat()
        }
    }

    /**
     * Calculate the dynamic effect, that one that follows the users finger
     */
    private fun doDynamicCurl() {
        val width = width
        val height = height

        // F will follow the finger, we add a small displacement
        // So that we can see the edge
        mF!!.x = width - mMovement!!.x + 0.1f
        mF!!.y = height - mMovement!!.y + 0.1f

        // Set min points
        if (mA!!.x == 0f) {
            mF!!.x = Math.min(mF!!.x, mOldF!!.x)
            mF!!.y = Math.max(mF!!.y, mOldF!!.y)
        }

        // Get diffs
        val deltaX = width - mF!!.x
        val deltaY = height - mF!!.y
        val BH = (Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()) / 2).toFloat()
        val tangAlpha = (deltaY / deltaX).toDouble()
        val alpha = Math.atan((deltaY / deltaX).toDouble())
        val _cos = Math.cos(alpha)
        val _sin = Math.sin(alpha)
        mA!!.x = (width - BH / _cos).toFloat()
        mA!!.y = height.toFloat()
        mD!!.y = (height - BH / _sin).toFloat()
        mD!!.x = width.toFloat()
        mA!!.x = Math.max(0f, mA!!.x)
        if (mA!!.x == 0f) {
            mOldF!!.x = mF!!.x
            mOldF!!.y = mF!!.y
        }

        // Get W
        mE!!.x = mD!!.x
        mE!!.y = mD!!.y

        // Correct
        if (mD!!.y < 0) {
            mD!!.x = width + (tangAlpha * mD!!.y).toFloat()
            mE!!.y = 0f
            mE!!.x = width + (Math.tan(2 * alpha) * mD!!.y).toFloat()
        }
    }

    /**
     * Swap between the fore and back-ground.
     */
    @Deprecated("")
    private fun SwapViews() {
        val temp = mForeground
        mForeground = mBackground
        mBackground = temp

        pageRunningListener.onCurrentPageRunning(mForeground)
    }

    /**
     * Swap to next view
     */
    private fun nextView() {
        var foreIndex = mIndex + 1
        if (foreIndex >= mPages.size) {
            foreIndex = 0
        }
        var backIndex = foreIndex + 1
        if (backIndex >= mPages.size) {
            backIndex = 0
        }
        mIndex = foreIndex
        setViews(foreIndex, backIndex)
    }

    /**
     * Swap to previous view
     */
    private fun previousView() {
        val backIndex = mIndex
        var foreIndex = backIndex - 1
        if (foreIndex < 0) {
            foreIndex = mPages.size - 1
        }
        mIndex = foreIndex
        setViews(foreIndex, backIndex)
    }

    /**
     * Set current fore and background
     * @param foreground - Foreground view index
     * @param background - Background view index
     */
    private fun setViews(foreground: Int, background: Int) {
        mForeground = mPages[foreground]
        mBackground = mPages[background]

        pageRunningListener.onCurrentPageRunning(mForeground)
    }

    //---------------------------------------------------------------
    // Drawing methods
    //---------------------------------------------------------------
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (mPages.isEmpty()) return
        // Always refresh offsets
        mCurrentLeft = left
        mCurrentTop = top

        // Translate the whole canvas
        //canvas.translate(mCurrentLeft, mCurrentTop);

        // We need to initialize all size data when we first draw the view
        if (!bViewDrawn) {
            bViewDrawn = true
            onFirstDrawEvent(canvas)
        }
        canvas.drawColor(context.getColor(R.color.white))

        // Curl pages
        //DoPageCurl();

        // Calculate center position
        val centerX = (width - actualContentWidth) / 2
        val centerY = (height - actualContentHeight) / 2

        // Define Rect to be centered
        val rect = Rect(
            centerX,
            centerY,
            centerX + actualContentWidth,
            centerY + actualContentHeight
        )

        val paint = Paint()

        // Draw elements inside centered rect
        drawForeground(canvas, rect, paint)
        drawBackground(canvas, rect, paint)
        drawCurlEdge(canvas)

        // Draw any debug info once we are done
        if (bEnableDebugMode) drawDebug(canvas)

        // Check if we can re-enable input
        if (bEnableInputAfterDraw) {
            bBlockTouchInput = false
            bEnableInputAfterDraw = false
        }

        // Restore canvas
        //canvas.restore();
    }

    /**
     * Called on the first draw event of the view
     * @param canvas
     */
    protected fun onFirstDrawEvent(canvas: Canvas?) {
        mFlipRadius = width.toFloat()
        ResetClipEdge()
        DoPageCurl()
    }

    /**
     * Draw the foreground
     * @param canvas
     * @param rect
     * @param paint
     */
    private fun drawForeground(canvas: Canvas, rect: Rect, paint: Paint) {
        if (mForeground == null) return
        canvas.drawBitmap(mForeground!!, null, rect, paint)
        drawPageNum(canvas, mIndex)
    }

    /**
     * Create a Path used as a mask to draw the background page
     * @return
     */
    private fun createBackgroundPath(): Path {
        val path = Path()
        path.moveTo(mA!!.x, mA!!.y)
        path.lineTo(mB!!.x, mB!!.y)
        path.lineTo(mC!!.x, mC!!.y)
        path.lineTo(mD!!.x, mD!!.y)
        path.lineTo(mA!!.x, mA!!.y)
        return path
    }

    /**
     * Draw the background image.
     * @param canvas
     * @param rect
     * @param paint
     */
    private fun drawBackground(canvas: Canvas, rect: Rect, paint: Paint) {
        val mask = createBackgroundPath()

        // Save current canvas so we do not mess it up
        canvas.save()
        canvas.clipPath(mask)
        canvas.drawBitmap(mBackground!!, null, rect, paint)

        // Draw the page number (first page is 1 in real life :D
        // there is no page number 0 hehe)
        drawPageNum(canvas, mIndex)
        canvas.restore()
    }

    /**
     * Creates a path used to draw the curl edge in.
     * @return
     */
    private fun createCurlEdgePath(): Path {
        val path = Path()
        path.moveTo(mA!!.x, mA!!.y)
        path.lineTo(mD!!.x, mD!!.y)
        path.lineTo(mE!!.x, mE!!.y)
        path.lineTo(mF!!.x, mF!!.y)
        path.lineTo(mA!!.x, mA!!.y)
        return path
    }

    /**
     * Draw the curl page edge
     * @param canvas
     */
    private fun drawCurlEdge(canvas: Canvas) {
        val path = createCurlEdgePath()
        canvas.drawPath(path, mCurlEdgePaint!!)
    }

    /**
     * Draw page num (let this be a bit more custom)
     * @param canvas
     * @param pageNum
     */
    private fun drawPageNum(canvas: Canvas, pageNum: Int) {
        mTextPaint!!.color = context.getColor(R.color.white)
        val pageNumText = "- $pageNum -"
        drawCentered(
            canvas,
            pageNumText,
            canvas.height - mTextPaint!!.textSize - 5,
            mTextPaint,
            mTextPaintShadow
        )
    }

    /**
     * Draw debug info
     * @param canvas
     */
    private fun drawDebug(canvas: Canvas) {
        val posX = 10f
        var posY = 20f
        val paint = Paint()
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        paint.color = context.getColor(R.color.black)
        canvas.drawCircle(mOrigin!!.x, mOrigin!!.y, width.toFloat(), paint)
        paint.strokeWidth = 3f
        paint.color = context.getColor(R.color.white)
        canvas.drawCircle(mOrigin!!.x, mOrigin!!.y, width.toFloat(), paint)
        paint.strokeWidth = 5f
        paint.color = context.getColor(R.color.black)
        canvas.drawLine(mOrigin!!.x, mOrigin!!.y, mMovement!!.x, mMovement!!.y, paint)
        paint.strokeWidth = 3f
        paint.color = context.getColor(R.color.white)
        canvas.drawLine(mOrigin!!.x, mOrigin!!.y, mMovement!!.x, mMovement!!.y, paint)
        posY = debugDrawPoint(canvas, "A", mA, Color.RED, posX, posY)
        posY = debugDrawPoint(canvas, "B", mB, Color.GREEN, posX, posY)
        posY = debugDrawPoint(canvas, "C", mC, Color.BLUE, posX, posY)
        posY = debugDrawPoint(canvas, "D", mD, Color.CYAN, posX, posY)
        posY = debugDrawPoint(canvas, "E", mE, Color.YELLOW, posX, posY)
        posY = debugDrawPoint(canvas, "F", mF, Color.LTGRAY, posX, posY)
        posY = debugDrawPoint(canvas, "Mov", mMovement, Color.DKGRAY, posX, posY)
        posY = debugDrawPoint(canvas, "Origin", mOrigin, Color.MAGENTA, posX, posY)
        posY = debugDrawPoint(canvas, "Finger", mFinger, Color.GREEN, posX, posY)
    }

    private fun debugDrawPoint(
        canvas: Canvas,
        name: String,
        point: Vector2D?,
        color: Int,
        posX: Float,
        posY: Float
    ): Float {
        return debugDrawPoint(
            canvas,
            name + " " + point.toString(),
            point!!.x,
            point.y,
            color,
            posX,
            posY
        )
    }

    private fun debugDrawPoint(
        canvas: Canvas,
        name: String,
        X: Float,
        Y: Float,
        color: Int,
        posX: Float,
        posY: Float
    ): Float {
        mTextPaint!!.color = color
        drawTextShadowed(canvas, name, posX, posY, mTextPaint, mTextPaintShadow)
        val paint = Paint()
        paint.strokeWidth = 5f
        paint.color = color
        canvas.drawPoint(X, Y, paint)
        return posY + 15
    }

    companion object {
        /** Our Log tag  */
        private const val TAG = "PageCurlView"

        /** Simple curl mode. Curl target will move only in one axis.  */
        const val CURLMODE_SIMPLE = 0

        /** Dynamic curl mode. Curl target will move on both X and Y axis.  */
        const val CURLMODE_DYNAMIC = 1
        //---------------------------------------------------------------
        // Debug draw methods
        //---------------------------------------------------------------
        /**
         * Draw a text with a nice shadow
         */
        fun drawTextShadowed(
            canvas: Canvas,
            text: String?,
            x: Float,
            y: Float,
            textPain: Paint?,
            shadowPaint: Paint?
        ) {
            canvas.drawText(text!!, x - 1, y, shadowPaint!!)
            canvas.drawText(text, x, y + 1, shadowPaint)
            canvas.drawText(text, x + 1, y, shadowPaint)
            canvas.drawText(text, x, y - 1, shadowPaint)
            canvas.drawText(text, x, y, textPain!!)
        }

        /**
         * Draw a text with a nice shadow centered in the X axis
         * @param canvas
         * @param text
         * @param y
         * @param textPain
         * @param shadowPaint
         */
        fun drawCentered(
            canvas: Canvas,
            text: String?,
            y: Float,
            textPain: Paint?,
            shadowPaint: Paint?
        ) {
            val posx = (canvas.width - textPain!!.measureText(text)) / 2
            drawTextShadowed(canvas, text, posx, y, textPain, shadowPaint)
        }
    }
}