package moe.ore.txhook.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import com.xuexiang.xui.widget.textview.autofit.AutoFitTextView
import moe.ore.tars.TarsBase.*
import moe.ore.test.ProtobufParser
import moe.ore.test.TarsParser
import moe.ore.txhook.databinding.FragmentPacketAnayseBinding
import moe.ore.txhook.databinding.FragmentPacketDataBinding
import moe.ore.txhook.databinding.FragmentPacketInfoBinding
import moe.ore.txhook.datas.PacketInfoData
import moe.ore.txhook.helper.toHexString
import moe.ore.txhook.more.copyText
import moe.ore.txhook.more.toast
import java.lang.Exception
import java.util.*

class PacketFragment(private val sectionNumber: Int, private val data: PacketInfoData) : Fragment() {
    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // println("unknown: $sectionNumber")
        when(sectionNumber) {
            1 -> {
                val binding = FragmentPacketInfoBinding.inflate(inflater, container, false)

                val copyListener = View.OnClickListener { (it as XUICommonListItemView).let { itemView ->
                    context?.copyText(itemView.detailText.toString())
                } }

                val groupListView = binding.groupListView

                val uinItem = groupListView.createItemView("Uin")
                uinItem.detailText = data.uin.toString()

                val cmdItem = groupListView.createItemView("Cmd")
                cmdItem.detailText = data.cmd

                val seqItem = groupListView.createItemView("Seq")
                seqItem.detailText = data.seq.toString()

                val timeItem = groupListView.createItemView("Time")
                timeItem.detailText = data.time.toString()

                val msgCookieItem = groupListView.createItemView("SessionId")
                msgCookieItem.detailText = data.sessionId.toHexString()

                val sizeItem = groupListView.createItemView("BufferSize")
                sizeItem.detailText = data.bufferSize.toString()

                val descItem = groupListView.createItemView("Desc")
                descItem.detailText = "????????????"

                XUIGroupListView.newSection(context)
                    .setTitle("????????????")
                    .addItemView(uinItem, copyListener)
                    .addItemView(cmdItem, copyListener)
                    .addItemView(seqItem, copyListener)
                    .addItemView(timeItem, copyListener)
                    .addItemView(msgCookieItem, copyListener)
                    .addItemView(sizeItem, copyListener)
                    .addItemView(descItem, copyListener)
                    .addTo(groupListView)

                if (data.packetType != -100) {
                // ??????packettype???-100??????????????????????????????????????????
                    // ????????????????????????hook??????packettype???????????????????????????????????????????????????????????????bundles??????????????????packettype
                    val packetTypeItem = groupListView.createItemView("PacketType")
                    packetTypeItem.detailText = data.packetType.toString() // ???????????? ??? 0a 0b

                    val encodeTypeItem = groupListView.createItemView("EncodeType")
                    packetTypeItem.detailText = data.encodeType.toString() // ???????????? 00 01 02 ?????????????????????defaultkey???sessionkey??????

                    XUIGroupListView.newSection(context)
                        .setTitle("????????????")
                        .addItemView(packetTypeItem, copyListener)
                        .addItemView(encodeTypeItem, copyListener)
                        .addTo(groupListView)
                }

                /*
                   from ???????????????????????????
                 */
                return binding.root
            }
            2 -> {
                val binding = FragmentPacketDataBinding.inflate(inflater, container, false)

                val dataView = binding.data
                dataView.typeface = XUI.getDefaultTypeface()

                var i = 0
                dataView.text = data.buffer.joinToString("") {
                    val str = (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase(Locale.getDefault())
                    i++
                    return@joinToString if (i == 8) {
                        i = 0
                        str + "\n"
                    } else "$str "
                } + "\n\n"

                dataView.setOnClickListener {
                    context?.copyText((it as AutoFitTextView).text.toString())
                }

                return binding.root
            }
            3 -> {
                val binding = FragmentPacketAnayseBinding.inflate(inflater, container, false)

                val buttonView = binding.buttonView

                if (data.bufferSize - 4 <= 0) {
                    buttonView.visibility = GONE
                    binding.emptyView.visibility = VISIBLE
                }


                var isLogin = false
                if (data.cmd.startsWith("wtlogin") || data.cmd.startsWith("wlogin")) {
                    binding.asPb.visibility = GONE
                    binding.asJce.text = "????????????"
                    isLogin = true
                }

                binding.json.setTextSize(18f)

                binding.asJce.setOnClickListener {
                    if (isLogin) { // ??????????????????

                    } else { // ????????????jce
                        try {
                            // toast.show("????????????")
                            val parser = TarsParser(data.buffer, 4)

                            binding.json.bindJson(parser.startParsing())

                            binding.dataView.visibility = VISIBLE
                            binding.buttonView.visibility = GONE

                            toast.show("????????????")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toast.show("????????????Jce????????????")
                        }
                    }
                }

                binding.asPb.setOnClickListener {
                    try {
                        // toast.show("????????????")
                        val parser = ProtobufParser(data.buffer, 4)

                        binding.json.bindJson(parser.startParsing())

                        binding.dataView.visibility = VISIBLE
                        binding.buttonView.visibility = GONE

                        toast.show("????????????")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast.show("????????????Pb????????????")
                    }
                }

                return binding.root
            }
        }
        return View(context)
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int, data: PacketInfoData): PacketFragment {
            return PacketFragment(sectionNumber, data).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}