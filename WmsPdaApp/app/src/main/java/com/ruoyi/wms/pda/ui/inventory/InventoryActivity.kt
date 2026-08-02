package com.ruoyi.wms.pda.ui.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import com.ruoyi.wms.pda.R
import com.ruoyi.wms.pda.data.api.ApiClient
import com.ruoyi.wms.pda.data.api.Inventory
import com.ruoyi.wms.pda.data.prefs.SessionManager
import com.ruoyi.wms.pda.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 库存查询。
 *
 * - 进入页面自动加载库存列表：getInventoryList(warehouseId)
 * - 支持按商品名称实时搜索过滤
 * - 列表展示：商品名称 / 库位 / 数量 / 容器号
 */
class InventoryActivity : BaseActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvInventory: RecyclerView
    private lateinit var tvEmpty: TextView

    /** 后端返回的全量库存 */
    private var allInventory: List<Inventory> = emptyList()
    /** 当前展示的过滤结果 */
    private val filtered = mutableListOf<Inventory>()
    private lateinit var adapter: InventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        tvTitle = findViewById(R.id.tvTitle)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        rvInventory = findViewById(R.id.rvInventory)
        tvEmpty = findViewById(R.id.tvEmpty)

        tvTitle.text = "库存查询"

        adapter = InventoryAdapter(filtered)
        rvInventory.layoutManager = LinearLayoutManager(this)
        rvInventory.adapter = adapter

        // 搜索框实时过滤
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString()?.trim() ?: "")
            }
        })
        btnSearch.setOnClickListener {
            applyFilter(etSearch.text.toString().trim())
        }

        loadInventory()
    }

    private fun loadInventory() {
        val warehouseId = SessionManager.warehouseId.takeIf { it != 0L }
        lifecycleScope.launch {
            try {
                showLoading("加载库存...")
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.getInventoryList(warehouseId).execute()
                }
                val body = resp.body()
                if (resp.isSuccessful && body != null && body.isSuccess) {
                    allInventory = body.data ?: emptyList()
                    applyFilter(etSearch.text.toString().trim())
                } else {
                    toastLong(body?.msg ?: "加载库存失败")
                }
            } catch (e: Exception) {
                toastLong("加载库存异常: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /** 按商品名称过滤（空关键字显示全部） */
    private fun applyFilter(keyword: String) {
        filtered.clear()
        if (keyword.isEmpty()) {
            filtered.addAll(allInventory)
        } else {
            filtered.addAll(allInventory.filter {
                it.materialName?.contains(keyword, ignoreCase = true) == true ||
                    it.materialCode?.contains(keyword, ignoreCase = true) == true
            })
        }
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------------
    // 库存列表适配器（使用系统 simple_list_item_2，无需额外布局）
    // ------------------------------------------------------------------

    inner class InventoryAdapter(
        private val rows: List<Inventory>
    ) : RecyclerView.Adapter<InventoryAdapter.VH>() {

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
            val inv = rows[position]
            holder.t1.text = "${inv.materialName ?: ""}（${inv.materialCode ?: ""}）"
            holder.t2.text = buildString {
                append("库位: ${inv.locationCode ?: "-"}")
                append("    数量: ${inv.quantity ?: 0}")
                if (!inv.containerNo.isNullOrEmpty()) {
                    append("    容器: ${inv.containerNo}")
                }
            }
        }

        override fun getItemCount(): Int = rows.size
    }
}
