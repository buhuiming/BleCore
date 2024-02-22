package com.bhm.support.sdk.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.bhm.support.sdk.R
import com.bhm.support.sdk.core.GridSpacingItemDecoration
import com.bhm.support.sdk.core.MyStaggeredGridLayoutManager
import com.bhm.support.sdk.utils.ViewUtil.isInvalidClick
import com.bhm.support.sdk.widget.ChoseView.CheckItemRecyclerAdapter.MyViewHolder
import com.noober.background.drawable.DrawableCreator
import java.io.Serializable

/**
 * 选择控件
 */
@Suppress("unused")
class ChoseView : LinearLayout {
    private var viewType = 0
    private var isCircularView = false
    private var isOnlyShow = false
    private var itemViewHeight = 0f
    private var itemViewWidth = 0f
    private var itemViewPaddingStart = 0f
    private var itemViewPaddingTop = 0f
    private var itemViewPaddingEnd = 0f
    private var itemViewPaddingBottom = 0f
    private var itemViewMargin = 0f
    private var itemViewCorners = 0f
    private var itemViewStrokeWidth = 0f
    private var cvGravity = 0
    private var choseStyle = 0
    private var unCheckBgColor = 0
    private var checkBgColor = 0
    private var unCheckTextColor = 0
    private var checkTextColor = 0
    private var unCheckStrokeColor = 0
    private var checkStrokeColor = 0
    private var itemViewTextSize = 0f
    private var spanCount = 0
    private var adapter: CheckItemRecyclerAdapter? = null
    private var listData: List<IdNameEntity>? = null
    private var selectCallBack1: SelectCallBack1? = null
    private var selectCallBack: SelectCallBack? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (null == attrs) {
            return
        }
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChoseView)
        //item的高度，为0则自适应
        itemViewHeight = typedArray.getDimension(R.styleable.ChoseView_itemViewHeight, 0f)
        //item的宽度，为0则自适应，当viewType=recyclerView即只有viewType=2时，这个值不需要设置
        itemViewWidth = typedArray.getDimension(R.styleable.ChoseView_itemViewWidth, 0f)
        itemViewPaddingStart =
            typedArray.getDimension(R.styleable.ChoseView_itemViewPaddingStart, 0f)
        itemViewPaddingTop = typedArray.getDimension(R.styleable.ChoseView_itemViewPaddingTop, 0f)
        itemViewPaddingEnd = typedArray.getDimension(R.styleable.ChoseView_itemViewPaddingEnd, 0f)
        itemViewPaddingBottom =
            typedArray.getDimension(R.styleable.ChoseView_itemViewPaddingBottom, 0f)
        //每个itemView之间的间隔
        itemViewMargin = typedArray.getDimension(R.styleable.ChoseView_itemViewMargin, 0f)
        //圆角
        itemViewCorners = typedArray.getDimension(R.styleable.ChoseView_itemViewCorners, 0f)
        //描边
        itemViewStrokeWidth = typedArray.getDimension(R.styleable.ChoseView_itemViewStrokeWidth, 0f)
        //fixed=0固定显示， scrollView=1单行横向滑动显示，recyclerView=2网格gridView方式显示
        viewType = typedArray.getInt(R.styleable.ChoseView_viewType, 0)
        //是否只显示，不能点击
        isOnlyShow = typedArray.getBoolean(R.styleable.ChoseView_isOnlyShow, false)
        //item是否圆形，圆形的时候，圆角无效，直径为高
        isCircularView = typedArray.getBoolean(R.styleable.ChoseView_isCircularView, false)
        //0左边，1右边，2居中，当viewType=recyclerView即只有viewType=2时，这个值不需要设置
        cvGravity = typedArray.getInt(R.styleable.ChoseView_gravity, 0)
        //single=0单选，选中后不能点击取消；singleUnNeeded=1 单选，选中后可以点击取消；multiple=2多选，选中后可以点击取消；
        choseStyle = typedArray.getInt(R.styleable.ChoseView_choseStyle, 0)
        //网格布局一行显示的个数，只有viewType=recyclerView即只有viewType=2才有效
        spanCount = typedArray.getInt(R.styleable.ChoseView_spanCount, 4)
        unCheckBgColor = typedArray.getColor(
            R.styleable.ChoseView_unCheckBgColor,
            ContextCompat.getColor(context, R.color.white)
        )
        checkBgColor = typedArray.getColor(
            R.styleable.ChoseView_checkBgColor,
            ContextCompat.getColor(context, R.color.color_main)
        )
        unCheckTextColor = typedArray.getColor(
            R.styleable.ChoseView_unCheckTextColor,
            ContextCompat.getColor(context, R.color.color_main)
        )
        checkTextColor = typedArray.getColor(
            R.styleable.ChoseView_checkTextColor,
            ContextCompat.getColor(context, R.color.color_2f)
        )
        unCheckStrokeColor = typedArray.getColor(R.styleable.ChoseView_unCheckStrokeColor, 0)
        checkStrokeColor = typedArray.getColor(R.styleable.ChoseView_checkStrokeColor, 0)
        if (unCheckStrokeColor == 0) {
            unCheckStrokeColor = unCheckBgColor
        }
        if (checkStrokeColor == 0) {
            checkStrokeColor = checkBgColor
        }
        itemViewTextSize =
            typedArray.getDimensionPixelSize(R.styleable.ChoseView_itemViewTextSize, 42).toFloat()
        typedArray.recycle()
    }

    fun setItemViewSize(itemViewWidth: Float, itemViewHeight: Float) {
        this.itemViewWidth = itemViewWidth
        this.itemViewHeight = itemViewHeight
    }

    fun setItemViewTextSize(itemViewTextSize: Float) {
        this.itemViewTextSize = itemViewTextSize
    }

    fun setItemViewPadding(
        itemViewPaddingStart: Float,
        itemViewPaddingTop: Int,
        itemViewPaddingEnd: Int,
        itemViewPaddingBottom: Int
    ) {
        this.itemViewPaddingStart = itemViewPaddingStart
        this.itemViewPaddingTop = itemViewPaddingTop.toFloat()
        this.itemViewPaddingEnd = itemViewPaddingEnd.toFloat()
        this.itemViewPaddingBottom = itemViewPaddingBottom.toFloat()
    }

    fun setItemViewMargin(itemViewMargin: Float) {
        this.itemViewMargin = itemViewMargin
    }

    fun setItemViewCorners(itemViewCorners: Float) {
        this.itemViewCorners = itemViewCorners
    }

    fun setItemViewStrokeWidth(itemViewStrokeWidth: Float) {
        this.itemViewStrokeWidth = itemViewStrokeWidth
    }

    fun setBgColor(checkBgColor: Int, unCheckBgColor: Int) {
        this.checkBgColor = checkBgColor
        this.unCheckBgColor = unCheckBgColor
    }

    fun setTextColor(checkTextColor: Int, unCheckTextColor: Int) {
        this.checkTextColor = checkTextColor
        this.unCheckTextColor = unCheckTextColor
    }

    fun setCheckStrokeColor(checkStrokeColor: Int, unCheckStrokeColor: Int) {
        this.checkStrokeColor = checkStrokeColor
        this.unCheckStrokeColor = unCheckStrokeColor
        if (unCheckStrokeColor == 0) {
            this.unCheckStrokeColor = unCheckBgColor
        }
        if (checkStrokeColor == 0) {
            this.checkStrokeColor = checkBgColor
        }
    }

    fun setViewType(viewType: Int) {
        this.viewType = viewType
    }

    override fun setGravity(cvGravity: Int) {
        this.cvGravity = cvGravity
    }

    fun setChoseStyle(choseStyle: Int) {
        this.choseStyle = choseStyle
    }

    fun setSpanCount(spanCount: Int) {
        this.spanCount = spanCount
    }

    fun setIsCircularView(isCircularView: Boolean) {
        this.isCircularView = isCircularView
    }

    fun setIsOnlyShow(isOnlyShow: Boolean) {
        this.isOnlyShow = isOnlyShow
    }

    fun setData(entities: List<IdNameEntity>?) {
        listData = entities
        if (viewType == 0) {
            //数量少，横向不超出一屏幕使用这个
            if (choseStyle == 0 || choseStyle == 1) {
                val radioGroup = RadioGroup(context)
                radioGroup.orientation = HORIZONTAL
                radioGroup.removeAllViews()
                removeAllViews()
                this.addView(radioGroup)
                val params = LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
                )
                when (cvGravity) {
                    0 -> {
                        params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    1 -> {
                        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    }
                    2 -> {
                        params.gravity = Gravity.CENTER
                    }
                }
                radioGroup.layoutParams = params
                val lp = RadioGroup.LayoutParams(itemViewWidth.toInt(), itemViewHeight.toInt())
                lp.gravity = Gravity.CENTER
                lp.setMargins(0, 0, itemViewMargin.toInt(), 0)
                for (entity in listData!!) {
                    var name = entity.name
                    if (TextUtils.isEmpty(name)) {
                        name = entity.title
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = entity.value
                    }
                    radioGroup.addView(getItemView0(name, entity.isCheck, entity.id), lp)
                }
                radioGroup.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
                    if (choseStyle == 0) {
                        //选中，再点击不能取消
                        for (i in 0 until group.childCount) {
                            val radioButton = group.getChildAt(i) as RadioButton
                            radioButton.setTextColor(if (radioButton.id == checkedId) checkTextColor else unCheckTextColor)
                            val drawable =
                                DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                                    .setPadding(
                                        itemViewPaddingStart,
                                        itemViewPaddingTop,
                                        itemViewPaddingEnd,
                                        itemViewPaddingBottom
                                    )
                                    .setSolidColor(if (radioButton.id == checkedId) checkBgColor else unCheckBgColor)
                                    .setStrokeColor(if (radioButton.id == checkedId) checkStrokeColor else unCheckStrokeColor)
                                    .setStrokeWidth(itemViewStrokeWidth)
                                    .build()
                            radioButton.background = drawable
                        }
                    } else {
                        for (i in 0 until group.childCount) {
                            val radioButton = group.getChildAt(i) as SingleRadioButton
                            radioButton.setTextColor(if (radioButton.id == checkedId) checkTextColor else unCheckTextColor)
                            val drawable =
                                DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                                    .setPadding(
                                        itemViewPaddingStart,
                                        itemViewPaddingTop,
                                        itemViewPaddingEnd,
                                        itemViewPaddingBottom
                                    )
                                    .setSolidColor(if (radioButton.id == checkedId) checkBgColor else unCheckBgColor)
                                    .setStrokeColor(if (radioButton.id == checkedId) checkStrokeColor else unCheckStrokeColor)
                                    .setStrokeWidth(itemViewStrokeWidth)
                                    .build()
                            radioButton.background = drawable
                        }
                    }
                    if (selectCallBack != null) {
                        selectCallBack!!.selectBack(singleCheckItemPosition)
                    }
                }
            } else if (choseStyle == 2) {
                val contentView = LinearLayout(context)
                contentView.orientation = HORIZONTAL
                contentView.removeAllViews()
                removeAllViews()
                this.addView(contentView)
                val params = LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
                )
                when (cvGravity) {
                    0 -> {
                        params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    1 -> {
                        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    }
                    2 -> {
                        params.gravity = Gravity.CENTER
                    }
                }
                contentView.layoutParams = params
                val lp = LayoutParams(itemViewWidth.toInt(), itemViewHeight.toInt())
                lp.gravity = Gravity.CENTER
                lp.setMargins(0, 0, itemViewMargin.toInt(), 0)
                for (entity in listData!!) {
                    var name = entity.name
                    if (TextUtils.isEmpty(name)) {
                        name = entity.title
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = entity.value
                    }
                    contentView.addView(getItemView2(name, entity.isCheck, entity.id), lp)
                }
            }
        } else if (viewType == 1) {
            if (choseStyle == 0 || choseStyle == 1) {
                val scrollView = HorizontalScrollView(context)
                scrollView.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                scrollView.isHorizontalScrollBarEnabled = false
                val radioGroup = RadioGroup(context)
                radioGroup.orientation = HORIZONTAL
                radioGroup.removeAllViews()
                scrollView.removeAllViews()
                removeAllViews()
                scrollView.addView(radioGroup)
                this.addView(scrollView)
                val params = FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
                )
                when (cvGravity) {
                    0 -> {
                        params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    1 -> {
                        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    }
                    2 -> {
                        params.gravity = Gravity.CENTER
                    }
                }
                radioGroup.layoutParams = params
                val lp = RadioGroup.LayoutParams(itemViewWidth.toInt(), itemViewHeight.toInt())
                lp.gravity = Gravity.CENTER
                lp.setMargins(0, 0, itemViewMargin.toInt(), 0)
                for (entity in listData!!) {
                    var name = entity.name
                    if (TextUtils.isEmpty(name)) {
                        name = entity.title
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = entity.value
                    }
                    radioGroup.addView(getItemView0(name, entity.isCheck, entity.id), lp)
                }
                radioGroup.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
                    if (choseStyle == 0) {
                        //选中，再点击不能取消
                        for (i in 0 until group.childCount) {
                            val radioButton = group.getChildAt(i) as RadioButton
                            radioButton.setTextColor(if (radioButton.id == checkedId) checkTextColor else unCheckTextColor)
                            val drawable =
                                DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                                    .setPadding(
                                        itemViewPaddingStart,
                                        itemViewPaddingTop,
                                        itemViewPaddingEnd,
                                        itemViewPaddingBottom
                                    )
                                    .setSolidColor(if (radioButton.id == checkedId) checkBgColor else unCheckBgColor)
                                    .setStrokeColor(if (radioButton.id == checkedId) checkStrokeColor else unCheckStrokeColor)
                                    .setStrokeWidth(itemViewStrokeWidth)
                                    .build()
                            radioButton.background = drawable
                        }
                    } else {
                        for (i in 0 until group.childCount) {
                            val radioButton = group.getChildAt(i) as SingleRadioButton
                            radioButton.setTextColor(if (radioButton.id == checkedId) checkTextColor else unCheckTextColor)
                            val drawable =
                                DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                                    .setPadding(
                                        itemViewPaddingStart,
                                        itemViewPaddingTop,
                                        itemViewPaddingEnd,
                                        itemViewPaddingBottom
                                    )
                                    .setSolidColor(if (radioButton.id == checkedId) checkBgColor else unCheckBgColor)
                                    .setStrokeColor(if (radioButton.id == checkedId) checkStrokeColor else unCheckStrokeColor)
                                    .setStrokeWidth(itemViewStrokeWidth)
                                    .build()
                            radioButton.background = drawable
                        }
                    }
                    if (selectCallBack != null) {
                        selectCallBack!!.selectBack(singleCheckItemPosition)
                    }
                }
            } else if (choseStyle == 2) {
                val scrollView = HorizontalScrollView(context)
                scrollView.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                scrollView.isHorizontalScrollBarEnabled = false
                val contentView = LinearLayout(context)
                contentView.orientation = HORIZONTAL
                scrollView.removeAllViews()
                contentView.removeAllViews()
                removeAllViews()
                scrollView.addView(contentView)
                this.addView(scrollView)
                val params = FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
                )
                when (cvGravity) {
                    0 -> {
                        params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    1 -> {
                        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    }
                    2 -> {
                        params.gravity = Gravity.CENTER
                    }
                }
                contentView.layoutParams = params
                val lp = LayoutParams(itemViewWidth.toInt(), itemViewHeight.toInt())
                lp.gravity = Gravity.CENTER
                lp.setMargins(0, 0, itemViewMargin.toInt(), 0)
                for (entity in listData!!) {
                    var name = entity.name
                    if (TextUtils.isEmpty(name)) {
                        name = entity.title
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = entity.value
                    }
                    contentView.addView(getItemView2(name, entity.isCheck, entity.id), lp)
                }
            }
        } else if (viewType == 2) {
            removeAllViews()
            val recyclerView = RecyclerView(context!!)
            recyclerView.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            this.addView(recyclerView)
            val manager = MyStaggeredGridLayoutManager(spanCount, OrientationHelper.VERTICAL)
            manager.setScrollEnabled(false)
            recyclerView.layoutManager = manager
            recyclerView.setHasFixedSize(false)
            recyclerView.addItemDecoration(
                GridSpacingItemDecoration(
                    spanCount,
                    itemViewMargin.toInt(),
                    false
                )
            )
            adapter = CheckItemRecyclerAdapter(listData, object : SelectCallBack {
                @SuppressLint("NotifyDataSetChanged")
                override fun selectBack(position: Int) {
                    if (choseStyle == 0) {
                        //单选，选中的不能取消
                        if (listData!![position].isCheck) {
                            return
                        }
                        for (itemEntity in listData!!) {
                            itemEntity.isCheck = false
                        }
                        listData!![position].isCheck = true
                        if (selectCallBack != null) {
                            selectCallBack!!.selectBack(singleCheckItemPosition)
                        }
                    } else if (choseStyle == 1) {
                        //单选，可以取消
                        for (itemEntity in listData!!) {
                            itemEntity.isCheck = false
                        }
                        if (!listData!![position].isCheck) {
                            listData!![position].isCheck = true
                        }
                        if (selectCallBack != null) {
                            selectCallBack!!.selectBack(singleCheckItemPosition)
                        }
                    } else {
                        //多选
                        listData!![position].isCheck = !listData!![position].isCheck
                        if (selectCallBack1 != null) {
                            selectCallBack1!!.selectBack(multipleCheckItemPosition)
                        }
                    }
                    adapter!!.notifyDataSetChanged()
                }
            })
            recyclerView.adapter = adapter
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshRecyclerView() {
        if (adapter != null) {
            adapter!!.notifyDataSetChanged()
        }
    }

    //单选
    val singleCheckItemPosition: Int
        get() {
            if (choseStyle == 0 || choseStyle == 1) {
                //单选
                when (viewType) {
                    0 -> {
                        val radioGroup = getChildAt(0) as RadioGroup
                        return radioGroup.checkedRadioButtonId
                    }
                    1 -> {
                        val scrollView = getChildAt(0) as HorizontalScrollView
                        val radioGroup = scrollView.getChildAt(0) as RadioGroup
                        return radioGroup.checkedRadioButtonId
                    }
                    2 -> {
                        for (entity in listData!!) {
                            if (entity.isCheck) {
                                return entity.id
                            }
                        }
                    }
                }
            }
            return -1
        }

    //多选
    val multipleCheckItemPosition: ArrayList<Int?>?
        get() {
            if (choseStyle == 2) {
                //多选
                val res = ArrayList<Int?>()
                when (viewType) {
                    0 -> {
                        val contentView = getChildAt(0) as LinearLayout
                        for (i in 0 until contentView.childCount) {
                            val checkBox = contentView.getChildAt(i) as CheckBox
                            if (checkBox.isChecked) {
                                res.add(checkBox.id)
                            }
                        }
                    }
                    1 -> {
                        val scrollView = getChildAt(0) as HorizontalScrollView
                        val contentView = scrollView.getChildAt(0) as LinearLayout
                        for (i in 0 until contentView.childCount) {
                            val checkBox = contentView.getChildAt(i) as CheckBox
                            if (checkBox.isChecked) {
                                res.add(checkBox.id)
                            }
                        }
                    }
                    2 -> {
                        for (entity in listData!!) {
                            if (entity.isCheck) {
                                res.add(entity.id)
                            }
                        }
                    }
                }
                return res
            }
            return null
        }

    fun setSelectCallBackSingle(selectCallBack: SelectCallBack?) {
        this.selectCallBack = selectCallBack
    }

    fun setSelectCallBackMultiple(selectCallBack: SelectCallBack1?) {
        selectCallBack1 = selectCallBack
    }

    private fun getItemView0(text: String?, isCheck: Boolean, id: Int): RadioButton {
        val rb: RadioButton = if (choseStyle == 0) {
            //选中，再点击不能取消
            RadioButton(context)
        } else {
            SingleRadioButton(context)
        }
        rb.text = text
        rb.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemViewTextSize)
        rb.gravity = Gravity.CENTER
        rb.setButtonDrawable(android.R.color.transparent)
        rb.setTextColor(if (isCheck) checkTextColor else unCheckTextColor)
        rb.setBackgroundColor(if (isCheck) checkBgColor else unCheckBgColor)
        rb.id = id
        if (isCircularView) {
            itemViewCorners = itemViewHeight
        }
        val drawable = DrawableCreator.Builder().setCornersRadius(itemViewCorners)
            .setPadding(
                itemViewPaddingStart,
                itemViewPaddingTop,
                itemViewPaddingEnd,
                itemViewPaddingBottom
            )
            .setSolidColor(if (isCheck) checkBgColor else unCheckBgColor)
            .setStrokeColor(if (isCheck) checkStrokeColor else unCheckStrokeColor)
            .setStrokeWidth(itemViewStrokeWidth)
            .build()
        rb.background = drawable
        rb.isChecked = isCheck
        rb.isEnabled = !isOnlyShow
        return rb
    }

    private fun getItemView2(text: String?, isCheck: Boolean, id: Int): CheckBox {
        val checkBox = CheckBox(context)
        checkBox.text = text
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemViewTextSize)
        checkBox.gravity = Gravity.CENTER
        checkBox.setButtonDrawable(android.R.color.transparent)
        checkBox.setTextColor(if (isCheck) checkTextColor else unCheckTextColor)
        checkBox.setBackgroundColor(if (isCheck) checkBgColor else unCheckBgColor)
        checkBox.id = id
        if (isCircularView) {
            itemViewCorners = itemViewHeight
        }
        val drawable = DrawableCreator.Builder().setCornersRadius(itemViewCorners)
            .setPadding(
                itemViewPaddingStart,
                itemViewPaddingTop,
                itemViewPaddingEnd,
                itemViewPaddingBottom
            )
            .setSolidColor(if (isCheck) checkBgColor else unCheckBgColor)
            .setStrokeColor(if (isCheck) checkStrokeColor else unCheckStrokeColor)
            .setStrokeWidth(itemViewStrokeWidth)
            .build()
        checkBox.background = drawable
        checkBox.isChecked = isCheck
        checkBox.isEnabled = !isOnlyShow
        checkBox.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            buttonView.setTextColor(if (isChecked) checkTextColor else unCheckTextColor)
            val drawable1 = DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                .setPadding(
                    itemViewPaddingStart,
                    itemViewPaddingTop,
                    itemViewPaddingEnd,
                    itemViewPaddingBottom
                )
                .setSolidColor(if (isChecked) checkBgColor else unCheckBgColor)
                .setStrokeColor(if (isCheck) checkStrokeColor else unCheckStrokeColor)
                .setStrokeWidth(itemViewStrokeWidth)
                .build()
            buttonView.background = drawable1
            if (selectCallBack1 != null) {
                selectCallBack1!!.selectBack(multipleCheckItemPosition)
            }
        }
        return checkBox
    }

    internal inner class CheckItemRecyclerAdapter(
        private val list: List<IdNameEntity>?,
        private val callBack: SelectCallBack
    ) : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val linearLayout = LinearLayout(context)
            val textView = TextView(context)
            val params: LayoutParams
            if (isCircularView) {
                itemViewCorners = itemViewHeight
                params = LayoutParams(itemViewHeight.toInt(), itemViewHeight.toInt())
            } else if (itemViewWidth == 0f){
                params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemViewHeight.toInt())
            } else {
                params = LayoutParams(itemViewWidth.toInt(), itemViewHeight.toInt())
            }
            linearLayout.addView(textView, params)
            textView.gravity = Gravity.CENTER
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemViewTextSize)
            textView.layoutParams = params
            textView.gravity = Gravity.CENTER
            return MyViewHolder(linearLayout, textView)
        }

        override fun onBindViewHolder(
            holder: MyViewHolder,
            @SuppressLint("RecyclerView") position: Int
        ) {
            if (isCircularView) {
                itemViewCorners = itemViewHeight - 1 //显得更圆一点
            }
            val drawable = DrawableCreator.Builder().setCornersRadius(itemViewCorners)
                .setPadding(
                    itemViewPaddingStart,
                    itemViewPaddingTop,
                    itemViewPaddingEnd,
                    itemViewPaddingBottom
                )
                .setSolidColor(if (list!![position].isCheck) checkBgColor else unCheckBgColor)
                .setStrokeColor(if (list[position].isCheck) checkStrokeColor else unCheckStrokeColor)
                .setStrokeWidth(itemViewStrokeWidth)
                .build()
            holder.textView.setTextColor(if (list[position].isCheck) checkTextColor else unCheckTextColor)
            holder.textView.setBackgroundColor(if (list[position].isCheck) checkBgColor else unCheckBgColor)
            holder.textView.background = drawable
            holder.textView.isEnabled = !isOnlyShow
            var name = list[position].name
            if (TextUtils.isEmpty(name)) {
                name = list[position].title
            }
            if (TextUtils.isEmpty(name)) {
                name = list[position].value
            }
            holder.textView.text = name
            holder.textView.setOnClickListener(OnClickListener { v ->
                if (isInvalidClick(v, 300)) {
                    return@OnClickListener
                }
                callBack.selectBack(position)
            })
        }

        override fun getItemCount(): Int {
            return list!!.size
        }

        internal inner class MyViewHolder(linearLayout: LinearLayout?, var textView: TextView) :
            RecyclerView.ViewHolder(
                linearLayout!!
            )
    }

    /**
     * RadioButton可以取消选中
     */
    internal class SingleRadioButton : AppCompatRadioButton {
        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
        )

        override fun toggle() {
            isChecked = !isChecked
            if (!isChecked) {
                (parent as RadioGroup).clearCheck()
            }
        }
    }

    interface SelectCallBack {
        fun selectBack(position: Int)
    }

    interface SelectCallBack1 {
        fun selectBack(positions: List<Int?>?)
    }

    class IdNameEntity : Serializable {
        constructor()
        constructor(id: Int, name: String?) {
            this.id = id
            this.name = name
        }

        constructor(id: Int, name: String?, check: Boolean) {
            this.id = id
            this.name = name
            isCheck = check
        }

        constructor(id: Int, name: String?, value: String?) {
            this.id = id
            this.name = name
            this.value = value
        }

        var id = 0
        var name: String? = null
        var isCheck = false
        var value: String? = null
        var title: String? = null
    }
}