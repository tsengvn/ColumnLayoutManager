package com.tsengvn.widget

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.*
import android.view.View
import android.view.ViewGroup

/**
 * @author hienngo
 * @since 28/10/18
 */
class ColumnLayoutManager(val context: Context, var columns: Int = 3) : RecyclerView.LayoutManager() {
    private var columnDatas : Array<ColumnData> = Array(columns, {i -> ColumnData(i) })
    private var viewCaches : SparseArray<ViewCache> = SparseArray(20)
    private var viewIndexMap : MutableMap<View, Int> = hashMapOf()
    private val displayMetrics : DisplayMetrics = context.resources.displayMetrics

    private var columnWidth = 0
    private var firstIndex = 0
    private var lastIndex = 0
    private var orientation = android.content.res.Configuration.ORIENTATION_PORTRAIT
    private var columnSpanLookup : ColumnAttributeLookup = DefaultColumnAttributeLookup(columns)


    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        orientation = view.resources.configuration.orientation
    }

    override fun generateDefaultLayoutParams() =
        RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)

    override fun canScrollVertically() = true

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount == 0) return 0
        val topView = findViewByPosition(0)

        val bottomView = findViewByPosition(frames.size-1)

        val topBoundReached = topView?.top ?: -1 >= 0
        val bottomBoundReached = if (bottomView != null) {bottomView.bottom <= getVerticalSpace()} else false

        var scrolled = 0

        if (dy > 0) { //content is scrolled up
            scrolled = if (bottomBoundReached) {
                val bottomOffset = getVerticalSpace() - getDecoratedBottom(bottomView) + paddingBottom
                Math.max(-dy, bottomOffset)
            } else -dy
            offsetChildrenVertical(scrolled)
            recycleTopViews(recycler)
            fillBottomView(recycler, state)
        } else if (dy < 0){ //content is scrolled down
            scrolled = if (topBoundReached) {
                val topOffset = -getDecoratedTop(topView) + paddingTop
                Math.min(-dy, topOffset)
            } else -dy

            offsetChildrenVertical(scrolled)
            recycleBottomViews(recycler)
            fillTopView(recycler, state)
        }

        return -scrolled
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (!state.didStructureChange()) return

        if (state.isPreLayout) {
            Logger.d("is pre layout: ${state.isPreLayout}")
        }

        Logger.d("onLayoutChildren, detach all views, reset view cache")
        detachAndScrapAttachedViews(recycler)

        columnWidth = (width - paddingRight - paddingLeft) / getColumnSize()
        columnDatas = Array(columns, {i -> ColumnData(i) })
        lastIndex = 0
        firstIndex = 0

        var isLastView: Boolean
        do {
            val view = obtainViewFromRecycler(recycler, lastIndex)
            addView(view)
            layoutView(lastIndex, view, columnWidth, isBottom = true)

            lastIndex++

            //check the min bottom of all columns
            //if it's bigger than view's height, stop render
            isLastView = findMinBottom() > height
        } while (!isLastView && lastIndex < frames.size && lastIndex - firstIndex < state.itemCount)
        lastIndex -= 1

    }

    private fun getColumnSize() = columns

    /**
     * Return the total available height minus paddings
     */
    private fun getVerticalSpace() = height - paddingBottom - paddingTop


    fun isLastRowVisible(): Boolean {
//        TODO("not implemented")
        return true
    }

    fun setTextSizeChanged(textSizeChanged: Boolean) {
//        TODO("not implemented")
    }

    fun findFirstVisiblePosition(partiallyVisible : Boolean): Int {
        return firstIndex
    }

    fun findLastVisiblePosition(partiallyVisible: Boolean): Int {
        return lastIndex
    }

    /**
     * Render view height, based on provided width
     */
    private fun getViewHeight(view: View, width: Int) : Int{
        if (view.layoutParams == null) {
            view.layoutParams = generateDefaultLayoutParams()
        }
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        when {
//            expectedHeight.isContainerHeight -> {
//                val height = (expectedHeight.number() / 100.0).toInt() * getVerticalSpace()
//                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
//                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
//                view.layoutParams.height  = height
//            }
//            expectedHeight.isDPHeight -> {
//                val height = Math.round(
//                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//                    expectedHeight.number().toFloat(), displayMetrics))
//                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
//                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
//                view.layoutParams.height  = height
//            }
//            expectedHeight.isFrameWidth -> {
//                val desiredFrameHeightAsPercentageOfFrameWidth = expectedHeight.number() / 100.0
//                val height = Math.round(width * desiredFrameHeightAsPercentageOfFrameWidth).toInt()
//
//                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
//                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
//                view.layoutParams.height  = height
//            }
//            expectedHeight.isIntrinsic -> {
//
//            }
//            else -> {
//                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
//                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//            }
//        }



        view.layoutParams.width = width
        view.measure(widthMeasureSpec, heightMeasureSpec)
        return getDecoratedMeasuredHeight(view)
    }

    /**
     * Get current bottom of provided column
     */
    private fun getBottomByColumn(columnStart: Int) : Int {
        val lastIndex = columnDatas[columnStart].viewIndexList.lastOrNull() ?: return 0

        return findViewByPosition(lastIndex)?.run { getDecoratedBottom(this) } ?: 0
    }

    /**
     * Get current top of provided column
     */
    private fun getTopByColumn(columnStart: Int)  : Int {
        val firstIndex = columnDatas[columnStart].viewIndexList.firstOrNull() ?: return 0

        return findViewByPosition(firstIndex)?.run { getDecoratedTop(this) } ?: 0
    }

    /**
     * This method must be called after we place a new view or remove a view.
     * It will update the view index list of the specific column.
     * If we are removing view, its view index will be removed from list
     * If we are adding view, its view index will be added to list
     * If the view we are adding is a last view in row, all the max bottom will be selected and apply to all columns.
     */
    private fun updateBottomByColumn(viewIndex : Int, columnStart : Int, columnSpan : Int, isRemoving : Boolean = false, isLastBlock : Boolean = false) {
        for (i in 0 until columnSpan) {
            if (columnStart + i < getColumnSize()) {
                if (isRemoving) {
                    columnDatas[columnStart+i].viewIndexList.remove(viewIndex)
                } else {
                    columnDatas[columnStart+i].viewIndexList.add(viewIndex)
                }
            }
        }

        if (isLastBlock) {
            //get the max bottom in all columns
            val maxBottom = findMaxBottom()

            //update this bottom to all the last block in all columns
            columnDatas
                .forEach {
                    val lastIndex = if (it.viewIndexList.isNotEmpty()) it.viewIndexList.last() else -1
                    findViewByPosition(lastIndex)?.apply {
                        val decoratedTop = getDecoratedTop(this)
                        viewCaches.get(lastIndex)?.apply {
                            height = maxBottom - decoratedTop
                        }
                        layoutDecorated(this, this.left, this.top, this.right, maxBottom)
                    }
                }
        }
    }

    private fun updateTopByColumn(viewIndex : Int, columnStart : Int, columnSpan : Int, isRemoving : Boolean = false) {
        for (i in 0 until columnSpan) {
            if (columnStart + i < getColumnSize()) {
                if (isRemoving) {
                    columnDatas[columnStart+i].viewIndexList.remove(viewIndex)
                } else {
                    columnDatas[columnStart+i].viewIndexList.add(0, viewIndex)
                }
            }
        }
    }

    private fun layoutView(viewIndex : Int, view : View, columnWidth : Int, isBottom : Boolean) {
        val columnSpan = columnSpanLookup.getColumnSpan(viewIndex)
        val columnStart = columnSpanLookup.getColumnStart(viewIndex)
        val isLastBlockInRow = columnSpanLookup.isLastBlockInRow(viewIndex)
        val left = columnStart * columnWidth
        val right = left + columnSpan * columnWidth

        var viewCache = viewCaches.get(viewIndex)
        if (viewCache == null) {
            viewCache = ViewCache(width = right-left, height = getViewHeight(view, right-left))
            viewCaches.put(viewIndex, viewCache)
        }

        val top : Int
        val bottom : Int
        if (isBottom) {
            top = getBottomByColumn(columnStart)
            bottom = top + viewCache.height
            layoutDecorated(view, left, top, right, bottom)
            updateBottomByColumn(viewIndex, columnStart, columnSpan, isLastBlock = isLastBlockInRow)
        } else {
            bottom = getTopByColumn(columnStart)
            top = bottom - viewCache.height
            layoutDecorated(view, left, top, right, bottom)
            updateTopByColumn(viewIndex, columnStart, columnSpan)
        }
    }

    private fun fillBottomView(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        var index = lastIndex + 1
        while (childCount < state.itemCount && index < state.itemCount) {
            val view = obtainViewFromRecycler(recycler, index)
            addView(view)
            layoutView(index, view, columnWidth, isBottom = columnSpanLookup.isLastBlockInRow(index))
            lastIndex = index++
            Logger.d("fillBottomView, first=$firstIndex, last=$lastIndex")
        }
    }

    private fun fillTopView(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        var index = firstIndex -1
        while (index >= 0 && childCount < state.itemCount) {
            val view = obtainViewFromRecycler(recycler, index)
            addView(view, 0)
            layoutView(index, view, columnWidth, isBottom = false)
            firstIndex = index--
            Logger.d("fillTopView, first=$firstIndex, last=$lastIndex")
        }
    }

    private fun recycleTopViews(recycler: RecyclerView.Recycler) {
        var recycled = false
        var viewBottom = 0
        do {
            getChildAt(0)?.run {
                val top = getDecoratedTop(this)
                val viewHeight = this.height
                viewBottom = top + viewHeight
                if (viewBottom < 0) { //view's bottom is scroll out of screen
                    removeAndRecycleView(this, recycler)
                    viewIndexMap[this]?.apply {
                        updateTopByColumn(this,
                            columnSpanLookup.getColumnStart(this),
                            columnSpanLookup.getColumnSpan(this),
                            true)
                        firstIndex++
                        recycled = true
                    }

                    Logger.d("recycleTopViews, view $this is recycled")
                }
            }

        } while (viewBottom < 0 && itemCount > 0)

        if (recycled) {
            Logger.d("recycleTopViews, done, first=$firstIndex")
        }
    }

    private fun recycleBottomViews(recycler: RecyclerView.Recycler) {
        var top = 0
        var recycled = false
        do {
            getChildAt(childCount-1)?.run {
                top = getDecoratedTop(this)
                if (top > getVerticalSpace()) { //view is out of screen
                    removeAndRecycleView(this, recycler)
                    viewIndexMap[this]?.apply {
                        updateBottomByColumn(this,
                            columnSpanLookup.getColumnStart(this),
                            columnSpanLookup.getColumnSpan(this),
                            true)
                        lastIndex--
                        recycled = true
                    }
                    Logger.d("recycleBottomViews, view $this is recycled")
                }
            }

        } while (top > height + paddingTop && itemCount > 0)
        if (recycled) {
            Logger.d("recycleBottomViews, done, last=$lastIndex")
        }
    }

    private fun obtainViewFromRecycler(recycler: RecyclerView.Recycler, index: Int) : View {
        val view = recycler.getViewForPosition(index)
        viewIndexMap[view] = index
        return view
    }

    private fun findMinBottom() : Int {
        return columnDatas.mapNotNull { it.viewIndexList.lastOrNull() }
            .map { findViewByPosition(it)?.run { getDecoratedBottom(this) } ?: 0 }
            .min() ?: 0
    }

    private fun findMaxBottom() : Int {
        return columnDatas.mapNotNull { it.viewIndexList.lastOrNull() }
            .map { findViewByPosition(it)?.run { getDecoratedBottom(this) } ?: 0 }
            .max() ?: 0
    }

    data class ColumnData(val index: Int, val viewIndexList: MutableList<Int> = mutableListOf())

    data class ViewCache(var width : Int = 0, var height: Int = 0)

}