package moe.ore.txhook

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.guidview.GuideCaseView
import moe.ore.txhook.app.TXApp
import moe.ore.txhook.databinding.ActivityMainBinding
import moe.ore.txhook.more.*
import moe.ore.txhook.ui.list.CatchingBaseAdapter
import moe.ore.txhook.ui.main.SectionsPagerAdapter

import moe.ore.txhook.datas.ProtocolDatas
import java.util.concurrent.atomic.AtomicBoolean
import android.view.animation.Animation

import android.view.animation.TranslateAnimation
import androidx.appcompat.content.res.AppCompatResources

import android.view.View
import android.widget.ImageButton
import moe.ore.txhook.catching.PacketService
import moe.ore.txhook.helper.*


class MainActivity : BaseActivity() {
    // private var isCatching: Boolean = false
    private val isChanging = AtomicBoolean(false)

    private lateinit var binding: ActivityMainBinding
    private var adapter: CatchingBaseAdapter? = null
    private lateinit var searchButton: ImageButton
    private var showingDialog: MaterialDialog? = null

    private var isExit = 0
    private val exitHandler: Handler by lazy {
        object : Handler(mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                isExit--
            }
        }
    }

    private var onSearch = false
    private val uiHandler: Handler by lazy { object :Handler(mainLooper) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                UI_CHANGE_SEARCH_BUTTON -> {
                    if (msg.arg1 == 1) {
                        searchButton.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.ic_baseline_search_24))
                    } else {
                        searchButton.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.ic_baseline_search_off_24))
                    }
                }
                UI_CHANGE_CATCHING_LIST -> {
                    changeContent(false, DebugUtil.forcedConvert(msg.obj)!!)
                }
                UI_FILTER_CATCHING_BY_MATCH -> {
                    filterCatchingList(msg.arg1, msg.obj as String)
                }


            }
        }
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, uiHandler, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = binding.tabs

        TXApp.catchingList.addAll(ProtocolDatas.getServices().let {
            if (it.size >= config.maxPacketSize + 10) it.slice(0 .. config.maxPacketSize) else it
        })

        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = binding.fab

        fab.setOnClickListener { (it as FloatingActionButton).also { changeContent(true) } }

        val deleteButton = binding.deleteAll
        this.searchButton = binding.searchView

        viewPager.addOnPageChangeListener(object :ViewPager.OnPageChangeListener {
            private var mHiddenAction: TranslateAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f)
            private var mShowAction: TranslateAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, -0.0f)

            init {
                mShowAction.repeatMode = Animation.REVERSE
                mShowAction.duration = 500
                mHiddenAction.duration = 500
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            private fun changeDeleteButton(isShow: Boolean) {
                if (deleteButton.visibility == INVISIBLE && isShow) {
                    deleteButton.clearAnimation()
                    deleteButton.startAnimation(mShowAction)
                    deleteButton.visibility = VISIBLE
                } else if (deleteButton.visibility == VISIBLE && !isShow) {
                    deleteButton.clearAnimation()
                    deleteButton.startAnimation(mHiddenAction)
                    deleteButton.visibility = INVISIBLE
                }
            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        if (config.changeViewRefresh) changeContent() // ?????????????????????
                        fab.show() // ??????fab??????
                        changeDeleteButton(true) // ????????????????????????
                        searchButton.visibility = VISIBLE
                    }
                    1 -> {
                        searchButton.visibility = GONE
                        TXApp.getCatchingSearchBar().closeSearch()
                        changeDeleteButton(true) // ????????????KEY??????
                        fab.hide()
                    }
                    else -> {
                        searchButton.visibility = GONE
                        TXApp.getCatchingSearchBar().closeSearch()
                        changeDeleteButton(false)
                        fab.hide()
                    }
                }
            }
        })

        deleteButton.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> {
                    ProtocolDatas.clearService()
                    toast.show("????????????????????????")
                    changeContent()
                }
                1 -> {
                    ProtocolDatas.emptyKeyList()
                    toast.show("KEYS????????????")
                }
            }
        }

        searchButton.setOnLongClickListener {
            MaterialDialog.Builder(this).title("????????????").items("??????????????????", "SEQ????????????", "??????????????????").itemsCallback { dialog: MaterialDialog?, _: View?, which: Int, _: CharSequence ->
                dialog?.dismiss()
                when(which) {
                    0 -> runOnUiThread {
                        MaterialDialog.Builder(this)
                            .iconRes(R.drawable.ic_baseline_manage_search_24)
                            .title("???????????????")
                            .content("??????????????????????????????????????????????????????Hex??????")
                            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                            .input("?????????????????????~~", "", false, (MaterialDialog.InputCallback { v1: MaterialDialog?, input: CharSequence ->
                                v1?.dismiss()
                                filterCatchingList(2, input.toString())
                            }))
                            .positiveText("??????")
                            .negativeText("??????")
                            .cancelable(true)
                            .show()
                    }
                    1 -> runOnUiThread {
                        MaterialDialog.Builder(this)
                            .iconRes(R.drawable.ic_baseline_manage_search_24)
                            .title("?????????SEQ????????????")
                            .content("??????SEQ?????????????????????????????????xxx-xxx???")
                            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                            .input("xxx-zzz", "", false, (MaterialDialog.InputCallback { v1: MaterialDialog?, input: CharSequence ->
                                v1?.dismiss()
                                filterCatchingList(3, input.toString())
                            }))
                            .positiveText("??????")
                            .negativeText("??????")
                            .cancelable(true)
                            .show()
                    }
                    2 -> runOnUiThread {
                        MaterialDialog.Builder(this)
                            .iconRes(R.drawable.ic_baseline_manage_search_24)
                            .title("??????????????????????????????")
                            .content("????????????????????????????????????????????????xxx-xxx???")
                            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                            .input("xxx-zzz", "", false, (MaterialDialog.InputCallback { v1: MaterialDialog?, input: CharSequence ->
                                v1?.dismiss()
                                filterCatchingList(4, input.toString())
                            }))
                            .positiveText("??????")
                            .negativeText("??????")
                            .cancelable(true)
                            .show()
                    }
                    else -> error("????????????????????????")
                }
            }.show()
            return@setOnLongClickListener false
        }

        searchButton.setOnClickListener {
            val searchBar = TXApp.getCatchingSearchBar()

            fun closeSearch() {
                searchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_search_off_24))
                onSearch = false
                if (searchBar.isSearchOpen) {
                    searchBar.closeSearch()
                    searchBar.visibility = GONE
                }
            }

            fun showSearch() {
                searchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_search_24))
                onSearch = true
                if (!searchBar.isSearchOpen) {
                    searchBar.showSearch()
                    searchBar.visibility = VISIBLE
                }
            }

            if (TXApp.catchingList.isNotEmpty()) {
                if (onSearch) closeSearch() else showSearch()
            } else toast.show("???????????????????????????????????????")

        }

        inputActivity()
    }

    private fun filterCatchingList(mode: Int, query: String) {
        var canContinue = true

        val sourceList = TXApp.catchingList
        val dialogBuilder = MaterialDialog.Builder(this).iconRes(R.drawable.ic_baseline_manage_search_24).limitIconToDefaultSize()
            .title("????????????")
            .content("??????????????????...")
            .progress(false, sourceList.size)
            .progressIndeterminateStyle(false)
            .negativeText("??????")
            .onNegative { dialog, _ ->
                dialog.dismiss()
                canContinue = false
                toast.show("????????????")
            }

        when (mode) {
            1 -> {
                val dialog = dialogBuilder.show()
                toast.show("??????????????????????????????|??????|??????")
                ThreadManager[0].addTask {
                    fastTry {
                        val result: ArrayList<PacketService> = arrayListOf()
                        val regex = query.toRegex()
                        sourceList.forEach {
                            if (canContinue) {
                                val cmd = if (it.to) it.toToService().cmd else it.toFromService().cmd
                                if (query == cmd || cmd.contains(query, true) || regex.matches(cmd)) {
                                    result.add(it)
                                }
                                dialog.incrementProgress(1)
                            } else {
                                dialog.dismiss()
                                return@addTask
                            }
                        }
                        dialog.dismiss()
                        if (result.isNotEmpty()) {
                            toast.show("???????????????${result.size}")
                            changeContent(false, result)
                        } else {
                            toast.show("???????????????????????????...")
                        }
                    }.onFailure {
                        toast.show(it.message)
                    }
                }
            }
            2 -> {
                toast.show("????????????????????????")
                val dialog = dialogBuilder.show()
                ThreadManager[0].addTask {
                    fastTry {
                        val result: ArrayList<PacketService> = arrayListOf()
                        val bytes = query.hex2ByteArray()
                        sourceList.forEach {
                            if (canContinue) {
                                val buffer = if (it.to) it.toToService().buffer else it.toFromService().buffer

                                if (BytesUtil.containBytes(buffer, bytes)) {
                                    result.add(it)
                                }

                                dialog.incrementProgress(1)
                            } else {
                                dialog.dismiss()
                                return@addTask
                            }
                        }
                        dialog.dismiss()
                        if (result.isNotEmpty()) {
                            toast.show("???????????????${result.size}")
                            changeContent(false, result)
                        } else {
                            toast.show("???????????????????????????...")
                        }
                    }.onFailure {
                        toast.show(it.message)
                    }
                }
            }
            3 -> {
                toast.show("SEQ????????????")
                val dialog = dialogBuilder.show()
                ThreadManager[0].addTask {
                    fastTry {
                        val result: ArrayList<PacketService> = arrayListOf()

                        val tmp = query.split("-")
                        val start = tmp[0].trim().toInt()
                        val end = tmp[1].trim().toInt()

                        val range = IntRange(start, end)

                        sourceList.forEach {
                            if (canContinue) {
                                val seq = if (it.to) it.toToService().seq else it.toFromService().seq

                                if (seq in range) {
                                    result.add(it)
                                }

                                dialog.incrementProgress(1)
                            } else {
                                dialog.dismiss()
                                return@addTask
                            }
                        }
                        dialog.dismiss()
                        if (result.isNotEmpty()) {
                            toast.show("???????????????${result.size}")
                            changeContent(false, result)
                        } else {
                            toast.show("???????????????????????????...")
                        }
                    }.onFailure {
                        toast.show(it.message)
                    }
                }
            }
            4 -> {
                toast.show("????????????????????????")
                val dialog = dialogBuilder.show()
                ThreadManager[0].addTask {
                    fastTry {
                        val result: ArrayList<PacketService> = arrayListOf()

                        val tmp = query.split("-")
                        val start = tmp[0].trim().toInt()
                        val end = tmp[1].trim().toInt()

                        val range = IntRange(start, end)

                        sourceList.forEach {
                            if (canContinue) {
                                val size = if (it.to) it.toToService().buffer.size else it.toFromService().buffer.size

                                if (size in range) {
                                    result.add(it)
                                }

                                dialog.incrementProgress(1)
                            } else {
                                dialog.dismiss()
                                return@addTask
                            }
                        }
                        dialog.dismiss()
                        if (result.isNotEmpty()) {
                            toast.show("???????????????${result.size}")
                            changeContent(false, result)
                        } else {
                            toast.show("???????????????????????????...")
                        }
                    }.onFailure {
                        toast.show(it.message)
                    }
                }
            }
        }


    }

    fun changeContent(isClick: Boolean = false, services: List<PacketService> = ProtocolDatas.getServices()) {
        if (!isChanging.get()) {
            ThreadManager.getInstance(0).addTask {
                isChanging.set(true)
                val catchingList = TXApp.getCatchingList()
                adapter = catchingList.adapter as CatchingBaseAdapter?
                runOnUiThread {
                    if (services.isNotEmpty()) {
                        toast.show("???????????????${services.size}")
                        adapter?.setItemFirst(services)
                        adapter?.notifyDataSetChanged()
                        TXApp.catching.multipleStatusView.showContent()
                    } else {
                        TXApp.catchingList.clear()
                        TXApp.catching.multipleStatusView.showEmpty()
                        toast.show("??????????????????~")
                    }
                }
                isChanging.set(false)
            }
        } else {
            if (isClick) CookieBars.cookieBar(this, "????????????", "????????????????????????~", "OK", null)
        }
    }

    private fun inputActivity() = requestPermission {
        // ???????????????????????????????????????????????????
        if (!config.isFirst) { // ?????????????????????
            MaterialDialog.Builder(this)
                .autoDismiss(false)
                .iconRes(R.drawable.icon_warning)
                .title("??????????????????")
                .content("???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????\n???????????????/sdcard/TXHook\n??????????????????????????????????????????????????????")
                .input("???????????????", "", false, (MaterialDialog.InputCallback { dialog: MaterialDialog?, input: CharSequence ->
                    if (input.toString() == "??????????????????????????????????????????") {
                        config.isFirst = true
                        config.apply()

                        dialog?.dismiss()
                        init(true)
                    } else {
                        toast.show("????????????")
                    } // ???????????????QQ???????????????
                }))
                .inputRange(14, 14)
                .positiveText("??????")
                .cancelable(false) // ????????????
                .show()
        } else {
            init(false)
        }
    }

    private fun init(isFirst: Boolean) {
        if (isFirst) {
            GuideCaseView.Builder(this)
                .focusOn(binding.fab)
                .focusCircleRadiusFactor(1.5)
                .title("??????????????????????????????")
                .focusBorderColor(Color.GREEN)
                .titleStyle(0, Gravity.CENTER)
                .fitWindowsAuto()
                .build()
                .show()
            // ??????????????????
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isExit++
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun exit() {
        if (isExit < 2) {
            toast("????????????????????????")
            exitHandler.sendEmptyMessageDelayed(0, 2000)
        } else {
            ActivityCollector.finishAll()
            super.onBackPressed()
        }
    }

    override fun requiredPermission(): Array<String> = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    override fun needInitTheme(): Boolean = false

    companion object {
        const val UI_CHANGE_SEARCH_BUTTON = 0 // ????????????????????????
        const val UI_CHANGE_CATCHING_LIST = 1
        const val UI_FILTER_CATCHING_BY_MATCH = 2

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}



