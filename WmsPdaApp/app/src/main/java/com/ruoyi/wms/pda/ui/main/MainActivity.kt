package com.ruoyi.wms.pda.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.ruoyi.wms.pda.R
import com.ruoyi.wms.pda.data.prefs.SessionManager
import com.ruoyi.wms.pda.ui.base.BaseActivity
import com.ruoyi.wms.pda.ui.inbound.InboundActivity
import com.ruoyi.wms.pda.ui.inventory.InventoryActivity
import com.ruoyi.wms.pda.ui.outbound.OutboundActivity
import com.ruoyi.wms.pda.ui.settings.SettingsActivity
import com.ruoyi.wms.pda.ui.login.LoginActivity

/**
 * 主界面：4 个大按钮网格。
 *
 * - 入库扫码 / 出库扫码 / 库存查询 / 设置
 * - 顶部标题栏显示当前登录用户名
 * - 硬件扫码已启用，仅消费不做处理
 */
class MainActivity : BaseActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var btnInbound: CardView
    private lateinit var btnOutbound: CardView
    private lateinit var btnInventory: CardView
    private lateinit var btnSettings: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUsername = findViewById(R.id.tvUsername)
        btnInbound = findViewById(R.id.btnInbound)
        btnOutbound = findViewById(R.id.btnOutbound)
        btnInventory = findViewById(R.id.btnInventory)
        btnSettings = findViewById(R.id.btnSettings)

        // 未登录则返回登录页
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 标题栏显示用户名
        tvUsername.text = "用户：${SessionManager.username}"

        btnInbound.setOnClickListener {
            startActivity(Intent(this, InboundActivity::class.java))
        }
        btnOutbound.setOnClickListener {
            startActivity(Intent(this, OutboundActivity::class.java))
        }
        btnInventory.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /** 硬件扫码仅消费，主界面不处理 */
    override fun onScanResult(code: String) {
        // consume only
    }
}
