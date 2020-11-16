package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.isEmpty
import androidx.core.view.ViewCompat
import androidx.core.view.children
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.*
import kotlin.properties.Delegates

class MarkdownContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var elements: List<MarkdownElement>
    private var layoutManager: LayoutManager = LayoutManager()

    //for restore
    private var ids = arrayListOf<Int>()

    var textSize by Delegates.observable(14f) { _, old, value ->
        if (value == old) return@observable
        children.forEach {
            it as IMarkdownView
            it.fontSize = value
        }

    }
    var isLoading: Boolean = true
    private val padding  = context.dpToIntPx(8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        children.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
            usedHeight += it.measuredHeight
        }
        usedHeight += paddingBottom
        setMeasuredDimension(width, usedHeight)
    }

//    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
//        children.filter { it !is MarkdownTextView } .forEach {
//            it.restoreHierarchyState(container)
//        }
//        dispatchThawSelfOnly(container)
//    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState()).apply {
//            childrenStates = saveChildViewStates()
            layout = layoutManager
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        if (state is SavedState) {
//                state.childrenStates?.let { restoreChildViewStates(it) }
            layoutManager = state.layout
        }
//        children.filter { it !is MarkdownTextView } .forEachIndexed { index, view ->
//            layoutManager.attachToParent(view, index)
//        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
//        children.filter { it !is MarkdownTextView } .forEachIndexed { index, view ->
//            layoutManager.attachToParent(view, index)
//        }
        children.filter { it !is MarkdownTextView } .forEach {
            it.saveHierarchyState(layoutManager.container)
        }
        dispatchFreezeSelfOnly(container)
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = paddingTop
        val bodyWidht = right - left - paddingLeft - paddingRight
        val left = paddingLeft
        val right = left + bodyWidht

        children.forEach {
            if (it is MarkdownTextView) {
                it.layout(
                    left - paddingLeft / 2,
                    usedHeight,
                    r - paddingRight / 2,
                    usedHeight + it.measuredHeight
                )
            } else {
                it.layout(
                    left,
                    usedHeight,
                    right,
                    usedHeight + it.measuredHeight
                )
            }
            usedHeight += it.measuredHeight
        }
    }

    fun setContent(content: List<MarkdownElement>) {
        elements = content
        var maxId = 1000
        var index = 1
        content.forEach { it ->
            when(it) {
                is MarkdownElement.Text -> {
                    val tv = MarkdownTextView(context, textSize).apply {
                        setPaddingOptionally(left = padding, right = padding)
                        setLineSpacing(fontSize * 0.5f, 1f)
                    }
                    MarkdownBuilder(context)
                        .markdownToSpan(it)
                        .let { ss ->
                            tv.setText(ss, TextView.BufferType.SPANNABLE)
                        }
                    tv.id = maxId++
                    addView(tv)
                }
                is MarkdownElement.Image -> {
                    val iv = MarkdownImageView(
                        context,
                        textSize,
                        it.image.url,
                        it.image.text,
                        it.image.atl
                    )
                    iv.id = maxId++
                    addView(iv)
                    layoutManager.attachToParent(iv, index++)
                }
                is MarkdownElement.Scroll -> {
                    val cv = MarkdownCodeView(
                        context,
                        textSize,
                        it.code.text
                    )
                    cv.id = maxId++
                    addView(cv)
                    layoutManager.attachToParent(cv, index++)
                }
            }
        }
    }

    fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }

        if (searchResult.isEmpty()) return

        val bounds = elements.map { it.bounds }
        val result = searchResult.groupByBounds(bounds)

        children.forEachIndexed { index, view ->
            view as IMarkdownView
            view.renderSearchResult(result[index], elements[index].offset)
        }
    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>?
    ) {
        searchPosition ?: return
        val bounds = elements.map { it.bounds }

        val index = bounds.indexOfFirst { (start, end) ->
            val boundRange = start..end
            val (startPos, endPos) = searchPosition
            startPos in boundRange && endPos in boundRange
        }

        if (index >= 0) {
            val view = getChildAt(index)
            view as IMarkdownView
            view.renderSearchPosition(searchPosition, elements[index].offset)
        }
    }

    fun clearSearchResult() {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
    }

    fun setCopyListener(listener: (String) -> Unit) {
        children.filterIsInstance<MarkdownCodeView>().forEach { view ->
            view.copyListener = listener
        }
    }

    internal class SavedState : BaseSavedState, Parcelable {

        lateinit var layout: LayoutManager
//        var childrenStates: SparseArray<Parcelable>? = null

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            layout = source.readParcelable(LayoutManager::class.java.classLoader)!!
//            childrenStates = source.readSparseArray<Parcelable>(javaClass.classLoader)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(layout, flags)
//            out.writeSparseArray(childrenStates as SparseArray<Any>)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    internal class LayoutManager() : Parcelable {
        var ids: MutableList<Int> = mutableListOf()
        var container: SparseArray<Parcelable> = SparseArray()

        constructor(parcel: Parcel) : this() {
            ids = parcel.readArrayList(Int::class.java.classLoader) as ArrayList<Int>
            container = parcel.readSparseArray<Parcelable>(this::class.java.classLoader) as SparseArray<Parcelable>
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeIntArray(ids.toIntArray())
            parcel.writeSparseArray(container)
        }

        fun attachToParent(view: View, index: Int) {
            if (container.isEmpty()) {
                view.id = ViewCompat.generateViewId()
                ids.add(view.id)
            } else {
                view.id = ids[index - 1]
                view.restoreHierarchyState(container)
            }
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<LayoutManager> {
            override fun createFromParcel(parcel: Parcel): LayoutManager {
                return LayoutManager(parcel)
            }

            override fun newArray(size: Int): Array<LayoutManager?> {
                return arrayOfNulls(size)
            }
        }

    }

}