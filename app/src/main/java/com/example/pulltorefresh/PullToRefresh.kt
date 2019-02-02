package com.example.testpullrefresh

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.*
import android.widget.*
import com.example.pulltorefresh.R
import java.text.SimpleDateFormat
import java.util.*

class PullToRefresh : ListView {

    private var scrollbarEnabled: Boolean = false
    private var bounceBackHeader: Boolean = false
    private var lockScrollWhileRefreshing: Boolean = false
    private var showLastUpdatedText: Boolean = false
    private var pullToRefreshText: String? = null
    private var releaseToRefreshText: String? = null
    private var refreshingText: String? = null
    private var lastUpdatedText: String? = null
    private var lastUpdatedDateFormat = SimpleDateFormat("dd/MM HH:mm")

    private var previousY: Float = 0.toFloat()
    private var headerPadding: Int = 0
    private var hasResetHeader: Boolean = false
    private var lastUpdated: Long = -1
    private var state: State? = null
    private var headerContainer: LinearLayout? = null
    private var header: RelativeLayout? = null
    private var flipAnimation: RotateAnimation? = null
    private var reverseFlipAnimation: RotateAnimation? = null
    private var image: ImageView? = null
    private var spinner: ProgressBar? = null
    private var text: TextView? = null
    private var lastUpdatedTextView: TextView? = null
    /*private var onItemClickListener: AdapterView.OnItemClickListener? = null
    private var onItemLongClickListener: AdapterView.OnItemLongClickListener? = null*/
    private var onRefreshListener: OnRefreshListener? = null

    private var mScrollStartY: Float = 0.toFloat()
    private val IDLE_DISTANCE = 5

    /**
     * @return If the list is in 'Refreshing' state
     */
    val isRefreshing: Boolean
        get() = state == State.REFRESHING

    private enum class State {
        PULL_TO_REFRESH,
        RELEASE_TO_REFRESH,
        REFRESHING
    }

    /**
     * Interface to implement when you want to get notified of 'pull to refresh'
     * events.
     * Call setOnRefreshListener(..) to activate an OnRefreshListener.
     */
    interface OnRefreshListener {

        /**
         * Method to be called when a refresh is requested
         */
        fun onRefresh()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    override fun setOnItemClickListener(onItemClickListener: AdapterView.OnItemClickListener?) {
        //this.onItemClickListener = onItemClickListener
    }

    override fun setOnItemLongClickListener(onItemLongClickListener: AdapterView.OnItemLongClickListener) {
        //this.onItemLongClickListener = onItemLongClickListener
    }

    /**
     * Activate an OnRefreshListener to get notified on 'pull to refresh'
     * events.
     *
     * @param onRefreshListener The OnRefreshListener to get notified
     */
    fun setOnRefreshListener(onRefreshListener: OnRefreshListener) {
        this.onRefreshListener = onRefreshListener
    }

    /**
     * Default is false. When lockScrollWhileRefreshing is set to true, the list
     * cannot scroll when in 'refreshing' mode. It's 'locked' on refreshing.
     *
     * @param lockScrollWhileRefreshing
     */
    fun setLockScrollWhileRefreshing(lockScrollWhileRefreshing: Boolean) {
        this.lockScrollWhileRefreshing = lockScrollWhileRefreshing
    }

    /**
     * Default is false. Show the last-updated date/time in the 'Pull ro Refresh'
     * header. See 'setLastUpdatedDateFormat' to set the date/time formatting.
     *
     * @param showLastUpdatedText
     */
    fun setShowLastUpdatedText(showLastUpdatedText: Boolean) {
        this.showLastUpdatedText = showLastUpdatedText
        if (!showLastUpdatedText) lastUpdatedTextView!!.visibility = View.GONE
    }

    /**
     * Default: "dd/MM HH:mm". Set the format in which the last-updated
     * date/time is shown. Meaningless if 'showLastUpdatedText == false (default)'.
     * See 'setShowLastUpdatedText'.
     *
     * @param lastUpdatedDateFormat
     */
    fun setLastUpdatedDateFormat(lastUpdatedDateFormat: SimpleDateFormat) {
        this.lastUpdatedDateFormat = lastUpdatedDateFormat
    }

    /**
     * Explicitly set the state to refreshing. This
     * is useful when you want to show the spinner and 'Refreshing' text when
     * the refresh was not triggered by 'pull to refresh', for example on start.
     */
    fun setRefreshing() {
        state = State.REFRESHING
        scrollTo(0, 0)
        setUiRefreshing()
        setHeaderPadding(0)
    }

    /**
     * Set the state back to 'pull to refresh'. Call this method when refreshing
     * the data is finished.
     */
    fun onRefreshComplete() {
        state = State.PULL_TO_REFRESH
        resetHeader()
        lastUpdated = System.currentTimeMillis()
    }

    /**
     * Change the label text on state 'Pull to Refresh'
     *
     * @param pullToRefreshText Text
     */
    fun setTextPullToRefresh(pullToRefreshText: String) {
        this.pullToRefreshText = pullToRefreshText
        if (state == State.PULL_TO_REFRESH) {
            text!!.text = pullToRefreshText
        }
    }

    /**
     * Change the label text on state 'Release to Refresh'
     *
     * @param releaseToRefreshText Text
     */
    fun setTextReleaseToRefresh(releaseToRefreshText: String) {
        this.releaseToRefreshText = releaseToRefreshText
        if (state == State.RELEASE_TO_REFRESH) {
            text!!.text = releaseToRefreshText
        }
    }

    /**
     * Change the label text on state 'Refreshing'
     *
     * @param refreshingText Text
     */
    fun setTextRefreshing(refreshingText: String) {
        this.refreshingText = refreshingText
        if (state == State.REFRESHING) {
            text!!.text = refreshingText
        }
    }

    private fun init() {
        isVerticalFadingEdgeEnabled = false

        headerContainer = LayoutInflater.from(context).inflate(R.layout.ptr_header, null) as LinearLayout
        header = headerContainer!!.findViewById(R.id.ptr_id_header) as RelativeLayout
        text = header!!.findViewById(R.id.ptr_id_text) as TextView
        lastUpdatedTextView = header!!.findViewById(R.id.ptr_id_last_updated) as TextView
        image = header!!.findViewById(R.id.ptr_id_image) as ImageView
        spinner = header!!.findViewById(R.id.ptr_id_spinner) as ProgressBar

        pullToRefreshText = context.getString(R.string.ptr_pull_to_refresh)
        releaseToRefreshText = context.getString(R.string.ptr_release_to_refresh)
        refreshingText = context.getString(R.string.ptr_refreshing)
        lastUpdatedText = context.getString(R.string.ptr_last_updated)

        flipAnimation =
            RotateAnimation(0f, -180f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        flipAnimation!!.interpolator = LinearInterpolator()
        flipAnimation!!.duration = ROTATE_ARROW_ANIMATION_DURATION.toLong()
        flipAnimation!!.fillAfter = true

        reverseFlipAnimation =
            RotateAnimation(-180f, 0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        reverseFlipAnimation!!.interpolator = LinearInterpolator()
        reverseFlipAnimation!!.duration = ROTATE_ARROW_ANIMATION_DURATION.toLong()
        reverseFlipAnimation!!.fillAfter = true

        addHeaderView(headerContainer)
        setState(State.PULL_TO_REFRESH)
        scrollbarEnabled = isVerticalScrollBarEnabled

        val vto = header!!.viewTreeObserver
        vto.addOnGlobalLayoutListener(PTROnGlobalLayoutListener())

        super.setOnItemClickListener(PTROnItemClickListener())
        super.setOnItemLongClickListener(PTROnItemLongClickListener())
    }

    private fun setHeaderPadding(padding: Int) {
        headerPadding = padding

        val mlp = header!!.layoutParams as ViewGroup.MarginLayoutParams
        mlp.setMargins(0, Math.round(padding.toFloat()), 0, 0)
        header!!.layoutParams = mlp
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (lockScrollWhileRefreshing && (state == State.REFRESHING || animation != null && !animation.hasEnded())) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (firstVisiblePosition == 0) {
                    previousY = event.y
                } else {
                    previousY = -1f
                }

                // Remember where have we started
                mScrollStartY = event.y
            }

            MotionEvent.ACTION_UP -> if (previousY != -1f && (state == State.RELEASE_TO_REFRESH || firstVisiblePosition == 0)) {
                when (state) {
                    PullToRefresh.State.RELEASE_TO_REFRESH -> {
                        setState(State.REFRESHING)
                        bounceBackHeader()
                    }

                    PullToRefresh.State.PULL_TO_REFRESH -> resetHeader()
                }
            }

            MotionEvent.ACTION_MOVE -> if (previousY != -1f && firstVisiblePosition == 0 && Math.abs(mScrollStartY - event.y) > IDLE_DISTANCE) {
                val y = event.y
                var diff = y - previousY
                if (diff > 0) diff /= PULL_RESISTANCE
                previousY = y

                val newHeaderPadding = Math.max(Math.round(headerPadding + diff), -header!!.height)

                if (newHeaderPadding != headerPadding && state != State.REFRESHING) {
                    setHeaderPadding(newHeaderPadding)

                    if (state == State.PULL_TO_REFRESH && headerPadding > 0) {
                        setState(State.RELEASE_TO_REFRESH)

                        image!!.clearAnimation()
                        image!!.startAnimation(flipAnimation)
                    } else if (state == State.RELEASE_TO_REFRESH && headerPadding < 0) {
                        setState(State.PULL_TO_REFRESH)

                        image!!.clearAnimation()
                        image!!.startAnimation(reverseFlipAnimation)
                    }
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun bounceBackHeader() {
        val yTranslate = if (state == State.REFRESHING)
            header!!.height - headerContainer!!.height
        else
            -headerContainer!!.height - headerContainer!!.top + paddingTop

        val bounceAnimation = TranslateAnimation(
            TranslateAnimation.ABSOLUTE, 0f,
            TranslateAnimation.ABSOLUTE, 0f,
            TranslateAnimation.ABSOLUTE, 0f,
            TranslateAnimation.ABSOLUTE, yTranslate.toFloat()
        )

        bounceAnimation.duration = BOUNCE_ANIMATION_DURATION.toLong()
        bounceAnimation.isFillEnabled = true
        bounceAnimation.fillAfter = false
        bounceAnimation.fillBefore = true
        bounceAnimation.interpolator = OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION)
        bounceAnimation.setAnimationListener(HeaderAnimationListener(yTranslate))

        startAnimation(bounceAnimation)
    }

    private fun resetHeader() {
        if (firstVisiblePosition > 0) {
            setHeaderPadding(-header!!.height)
            setState(State.PULL_TO_REFRESH)
            return
        }

        if (animation != null && !animation.hasEnded()) {
            bounceBackHeader = true
        } else {
            bounceBackHeader()
        }
    }

    private fun setUiRefreshing() {
        spinner!!.visibility = View.VISIBLE
        image!!.clearAnimation()
        image!!.visibility = View.INVISIBLE
        text!!.text = refreshingText
    }

    private fun setState(state: State) {
        this.state = state
        when (state) {
            PullToRefresh.State.PULL_TO_REFRESH -> {
                spinner!!.visibility = View.INVISIBLE
                image!!.visibility = View.VISIBLE
                text!!.text = pullToRefreshText

                if (showLastUpdatedText && !lastUpdated.equals(-1)) {
                    lastUpdatedTextView!!.visibility = View.VISIBLE
                    lastUpdatedTextView!!.text =
                        String.format(lastUpdatedText!!, lastUpdatedDateFormat.format(Date(lastUpdated)))
                }
            }

            PullToRefresh.State.RELEASE_TO_REFRESH -> {
                spinner!!.visibility = View.INVISIBLE
                image!!.visibility = View.VISIBLE
                text!!.text = releaseToRefreshText
            }

            PullToRefresh.State.REFRESHING -> {
                setUiRefreshing()

                lastUpdated = System.currentTimeMillis()
                if (onRefreshListener == null) {
                    setState(State.PULL_TO_REFRESH)
                } else {
                    onRefreshListener!!.onRefresh()
                }
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (!hasResetHeader) {
            if (measuredHeaderHeight > 0 && state != State.REFRESHING) {
                setHeaderPadding(-measuredHeaderHeight)
            }

            hasResetHeader = true
        }
    }

    private inner class HeaderAnimationListener(private val translation: Int) : Animation.AnimationListener {

        private var height: Int = 0
        private var stateAtAnimationStart: State? = null

        override fun onAnimationStart(animation: Animation) {
            stateAtAnimationStart = state

            val lp = layoutParams
            height = lp.height
            lp.height = getHeight() - translation
            layoutParams = lp

            if (scrollbarEnabled) {
                isVerticalScrollBarEnabled = false
            }
        }

        override fun onAnimationEnd(animation: Animation) {
            setHeaderPadding(if (stateAtAnimationStart == State.REFRESHING) 0 else -measuredHeaderHeight - headerContainer!!.top)
            setSelection(0)

            val lp = layoutParams
            lp.height = height
            layoutParams = lp

            if (scrollbarEnabled) {
                isVerticalScrollBarEnabled = true
            }

            if (bounceBackHeader) {
                bounceBackHeader = false

                postDelayed({ resetHeader() }, BOUNCE_ANIMATION_DELAY.toLong())
            } else if (stateAtAnimationStart != State.REFRESHING) {
                setState(State.PULL_TO_REFRESH)
            }
        }

        override fun onAnimationRepeat(animation: Animation) {}
    }

    private inner class PTROnGlobalLayoutListener : ViewTreeObserver.OnGlobalLayoutListener {

        override fun onGlobalLayout() {
            val initialHeaderHeight = header!!.height

            if (initialHeaderHeight > 0) {
                measuredHeaderHeight = initialHeaderHeight

                if (measuredHeaderHeight > 0 && state != State.REFRESHING) {
                    setHeaderPadding(-measuredHeaderHeight)
                    requestLayout()
                }
            }

            viewTreeObserver.removeGlobalOnLayoutListener(this)
        }
    }

    private inner class PTROnItemClickListener : AdapterView.OnItemClickListener {

        override fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
            hasResetHeader = false

            if (onItemClickListener != null && state == State.PULL_TO_REFRESH) {
                // Passing up onItemClick. Correct position with the number of header views
                onItemClickListener!!.onItemClick(adapterView, view, position - headerViewsCount, id)
            }
        }
    }

    private inner class PTROnItemLongClickListener : AdapterView.OnItemLongClickListener {

        override fun onItemLongClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
            hasResetHeader = false

            return if (onItemLongClickListener != null && state == State.PULL_TO_REFRESH) {
                // Passing up onItemLongClick. Correct position with the number of header views
                onItemLongClickListener!!.onItemLongClick(adapterView, view, position - headerViewsCount, id)
            } else false

        }
    }

    companion object {

        private val PULL_RESISTANCE = 1.7f
        private val BOUNCE_ANIMATION_DURATION = 700
        private val BOUNCE_ANIMATION_DELAY = 100
        private val BOUNCE_OVERSHOOT_TENSION = 1.4f
        private val ROTATE_ARROW_ANIMATION_DURATION = 250

        private var measuredHeaderHeight: Int = 0
    }
}
