package com.ruoyi.wms.pda.ui.outbound

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import com.ruoyi.wms.pda.R
import com.ruoyi.wms.pda.data.api.ApiClient
import com.ruoyi.wms.pda.data.api.Location
import com.ruoyi.wms.pda.data.api.ShipmentOrderBo
import com.ruoyi.wms.pda.data.api.ShipmentOrderLine
import com.ruoyi.wms.pda.data.prefs.SessionManager
import com.ruoyi.wms.pda.ui.base.BaseActivity
import com.ruoyi.wms.pda.utils.ScanUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 出库扫码（核心功能）。
 *
 * - 出库单号本地自动生成："CK" + 时间戳
 * - 起点库位：getOccupiedLocations()
 * - 目标库位：getShipmentEndLocations()
 * - 容器号：选择起点库位后，通过 getContainerByLocation() 自动带出
 * - 商品：扫码或手工录入（商品名称/数量/金额），添加到列表
 * - 完成出库：调用 shipment(ShipmentOrderBo)
 *
 * 扫码处理同入库：硬件扫码填入聚焦输入框，摄像头扫码填入指定输入框。
 */
class OutboundActivity : BaseActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etOrderNo: EditText
    private lateinit var etContainerNo: EditText
    private lateinit var etSkuCode: EditText
    private lateinit var etMaterialName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etAmount: EditText
    private lateinit var spStartLocation: Spinner
    private lateinit var spTargetLocation: Spinner
    private lateinit var btnScanContainer: Button
    private lateinit var btnScanStartLocation: Button
    private lateinit var btnScanTargetLocation: Button
    private lateinit var btnScanSku: Button
    private lateinit var btnAddItem: Button
    private lateinit var btnComplete: Button
    private lateinit var rvItems: RecyclerView

    /** 列表显示行（与 API 模型解耦，便于展示金额） */
    data class ItemRow(
        val skuCode: String,
        val materialName: String,
        val quantity: Double,
        val amount: Double
    )

    private val items = mutableListOf<ItemRow>()
    private lateinit var itemAdapter: ItemAdapter

    private var occupiedLocations: List<Location> = emptyList()
    private var endLocations: List<Location> = emptyList()

    /** 摄像头扫码目标控件 */
    private var scanTargetView: View? = null

    /** 标记数据是否就绪，避免监听器在数据未加载时误触发 */
    private var startLocationReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outbound)

        tvTitle = findViewById(R.id.tvTitle)
        etOrderNo = findViewById(R.id.etOrderNo)
        etContainerNo = findViewById(R.id.etContainerNo)
        etSkuCode = findViewById(R.id.etSkuCode)
        etMaterialName = findViewById(R.id.etMaterialName)
        etQuantity = findViewById(R.id.etQuantity)
        etAmount = findViewById(R.id.etAmount)
        spStartLocation = findViewById(R.id.spStartLocation)
        spTargetLocation = findViewById(R.id.spTargetLocation)
        btnScanContainer = findViewById(R.id.btnScanContainer)
        btnScanStartLocation = findViewById(R.id.btnScanStartLocation)
        btnScanTargetLocation = findViewById(R.id.btnScanTargetLocation)
        btnScanSku = findViewById(R.id.btnScanSku)
        btnAddItem = findViewById(R.id.btnAddItem)
        btnComplete = findViewById(R.id.btnComplete)
        rvItems = findViewById(R.id.rvItems)

        tvTitle.text = "出库扫码"

        // 自动生成出库单号
        etOrderNo.setText(generateOrderNo())
        etOrderNo.isEnabled = false
        // 容器号由起点库位带出，初始不可编辑
        etContainerNo.isEnabled = false

        // 商品列表
        itemAdapter = ItemAdapter(items) { pos -> removeItem(pos) }
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = itemAdapter

        // 起点库位变化时自动获取容器号
        spStartLocation.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if (!startLocationReady) return
                    if (position in occupiedLocations.indices) {
                        val code = occupiedLocations[position].locationCode ?: ""
                        if (code.isNotEmpty()) {
                            loadContainerByLocation(code)
                        }
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // 加载库位
        loadStartLocations()
        loadEndLocations()

        // 摄像头扫码按钮
        btnScanStartLocation.setOnClickListener {
            scanTargetView = spStartLocation
            ScanUtil.startScan(this)
        }
        btnScanTargetLocation.setOnClickListener {
            scanTargetView = spTargetLocation
            ScanUtil.startScan(this)
        }
        btnScanContainer.setOnClickListener {
            scanTargetView = etContainerNo
            ScanUtil.startScan(this)
        }
        btnScanSku.setOnClickListener {
            scanTargetView = etSkuCode
            ScanUtil.startScan(this)
        }

        btnAddItem.setOnClickListener { addItem() }
        btnComplete.setOnClickListener { completeOutbound() }
    }

    // ------------------------------------------------------------------
    // 自动生成
    // ------------------------------------------------------------------

    private fun generateOrderNo(): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return "CK" + sdf.format(Date())
    }

    private fun loadStartLocations() {
        lifecycleScope.launch {
            try {
                showLoading("加载起点库位...")
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.getOccupiedLocations().execute()
                }
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.isSuccess) {
                    occupiedLocations = body.data ?: emptyList()
                    bindLocationSpinner(spStartLocation, occupiedLocations)
                    startLocationReady = true
                    // 选中首项后主动触发一次容器号获取
                    if (occupiedLocations.isNotEmpty()) {
                        loadContainerByLocation(occupiedLocations[0].locationCode ?: "")
                    }
                }
            } catch (e: Exception) {
                toastLong("加载起点库位失败: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    private fun loadEndLocations() {
        lifecycleScope.launch {
            try {
                showLoading("加载目标库位...")
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.getShipmentEndLocations().execute()
                }
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.isSuccess) {
                    endLocations = body.data ?: emptyList()
                    bindLocationSpinner(spTargetLocation, endLocations)
                }
            } catch (e: Exception) {
                toastLong("加载目标库位失败: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /** 根据起点库位编码获取容器号并自动填入 */
    private fun loadContainerByLocation(locationCode: String) {
        lifecycleScope.launch {
            try {
                showLoading("获取容器号...")
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.getContainerByLocation(locationCode).execute()
                }
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.isSuccess) {
                    etContainerNo.setText(body.data ?: "")
                } else {
                    etContainerNo.setText("")
                }
            } catch (e: Exception) {
                toastLong("获取容器号失败: ${e.message}")
                etContainerNo.setText("")
            } finally {
                hideLoading()
            }
        }
    }

    private fun bindLocationSpinner(spinner: Spinner, locations: List<Location>) {
        val codes = locations.map { it.locationCode ?: "" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, codes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun selectSpinnerByCode(spinner: Spinner, locations: List<Location>, code: String) {
        val idx = locations.indexOfFirst { it.locationCode == code }
        if (idx >= 0) {
            spinner.setSelection(idx)
            toast("已选择库位: $code")
        } else {
            toastLong("未找到库位: $code")
        }
    }

    // ------------------------------------------------------------------
    // 商品列表
    // ------------------------------------------------------------------

    private fun addItem() {
        val skuCode = etSkuCode.text.toString().trim()
        val materialName = etMaterialName.text.toString().trim()
        val qtyStr = etQuantity.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()

        if (TextUtils.isEmpty(materialName)) {
            toast("请输入商品名称"); return
        }
        if (TextUtils.isEmpty(qtyStr)) {
            toast("请输入数量"); return
        }
        val qty = qtyStr.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            toast("数量不合法"); return
        }
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        items.add(ItemRow(skuCode, materialName, qty, amount))
        itemAdapter.notifyItemInserted(items.size - 1)

        etSkuCode.setText("")
        etMaterialName.setText("")
        etQuantity.setText("")
        etAmount.setText("")
        etSkuCode.requestFocus()
        toast("已添加：$materialName")
    }

    private fun removeItem(position: Int) {
        if (position < 0 || position >= items.size) return
        items.removeAt(position)
        itemAdapter.notifyItemRemoved(position)
        itemAdapter.notifyItemRangeChanged(position, items.size - position)
        toast("已删除该项")
    }

    // ------------------------------------------------------------------
    // 完成出库
    // ------------------------------------------------------------------

    private fun completeOutbound() {
        if (items.isEmpty()) {
            toast("请先添加商品"); return
        }
        val containerNo = etContainerNo.text.toString().trim()
        if (TextUtils.isEmpty(containerNo)) {
            toast("未获取到容器号"); return
        }
        val startIdx = spStartLocation.selectedItemPosition
        if (startIdx < 0 || startIdx >= occupiedLocations.size) {
            toast("请选择起点库位"); return
        }
        val startLoc = occupiedLocations[startIdx]
        val orderNo = etOrderNo.text.toString().trim()

        val lines = items.map { row ->
            ShipmentOrderLine(
                materialCode = row.skuCode,
                materialName = row.materialName,
                realQuantity = row.quantity,
                locationId = startLoc.id,
                locationCode = startLoc.locationCode,
                containerNo = containerNo
            )
        }
        val warehouseId = SessionManager.warehouseId.takeIf { it != 0L }
        val bo = ShipmentOrderBo(
            id = 0L,
            orderNo = orderNo,
            warehouseId = warehouseId,
            detailList = lines
        )

        lifecycleScope.launch {
            try {
                showLoading("提交出库...")
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.shipment(bo).execute()
                }
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.isSuccess) {
                    toastLong("出库成功")
                    finish()
                } else {
                    toastLong(body?.msg ?: "出库失败")
                }
            } catch (e: Exception) {
                toastLong("出库异常: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    // ------------------------------------------------------------------
    // 扫码处理
    // ------------------------------------------------------------------

    /** 硬件扫码：填入当前聚焦的输入框，否则作为 SKU 条码 */
    override fun onScanResult(code: String) {
        applyScanResult(code, currentFocus)
    }

    /** 摄像头扫码结果 */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = ScanUtil.parseResult(requestCode, resultCode, data)
        if (result != null) {
            applyScanResult(result, scanTargetView)
            scanTargetView = null
        }
    }

    private fun applyScanResult(code: String, targetView: View?) {
        when (targetView) {
            etContainerNo -> etContainerNo.setText(code)
            etSkuCode -> etSkuCode.setText(code)
            etMaterialName -> etMaterialName.setText(code)
            etQuantity -> etQuantity.setText(code)
            etAmount -> etAmount.setText(code)
            spStartLocation -> selectSpinnerByCode(spStartLocation, occupiedLocations, code)
            spTargetLocation -> selectSpinnerByCode(spTargetLocation, endLocations, code)
            else -> {
                etSkuCode.setText(code)
                etSkuCode.requestFocus()
            }
        }
    }

    // ------------------------------------------------------------------
    // 商品列表适配器（使用系统 simple_list_item_2，无需额外布局）
    // ------------------------------------------------------------------

    inner class ItemAdapter(
        private val rows: List<ItemRow>,
        private val onLongClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ItemAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val t1: TextView = v.findViewById(android.R.id.text1)
            val t2: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.t1.text = "${position + 1}. ${row.materialName}"
            holder.t2.text = "数量: ${row.quantity}    金额: ${row.amount}"
            holder.itemView.setOnLongClickListener {
                onLongClick(holder.bindingAdapterPosition)
                true
            }
        }

        override fun getItemCount(): Int = rows.size
    }
}
