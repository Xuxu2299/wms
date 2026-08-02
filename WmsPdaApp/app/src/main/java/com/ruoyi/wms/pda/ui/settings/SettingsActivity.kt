package com.ruoyi.wms.pda.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.ruoyi.wms.pda.R
import com.ruoyi.wms.pda.data.api.ApiClient
import com.ruoyi.wms.pda.data.prefs.SessionManager
import com.ruoyi.wms.pda.ui.base.BaseActivity
import com.ruoyi.wms.pda.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页。
 *
 * - 服务器地址输入与保存
 * - 退出登录（调用 logout 接口，清除会话，返回登录页）
 * - 显示当前登录用户
 */
class SettingsActivity : BaseActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etServerUrl: EditText
    private lateinit var tvCurrentUser: TextView
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvTitle = findViewById(R.id.tvTitle)
        etServerUrl = findViewById(R.id.etServerUrl)
        tvCurrentUser = findViewById(R.id.tvCurrentUser)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)

        tvTitle.text = "设置"

        // 显示当前服务器地址与用户
        etServerUrl.setText(SessionManager.serverUrl)
        tvCurrentUser.text = "当前用户：${SessionManager.username}"

        btnSave.setOnClickListener { saveServerUrl() }
        btnLogout.setOnClickListener { logout() }
    }

    private fun saveServerUrl() {
        val url = etServerUrl.text.toString().trim()
        if (TextUtils.isEmpty(url)) {
            toast("请输入服务器地址"); return
        }
        SessionManager.serverUrl = url
        ApiClient.recreate()
        toast("服务器地址已保存")
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                showLoading("退出登录...")
                withContext(Dispatchers.IO) {
                    ApiClient.api.logout().execute()
                }
            } catch (_: Exception) {
                // 即使接口失败也继续本地清理
            } finally {
                hideLoading()
            }
            // 清除会话（保留服务器地址）
            SessionManager.clear(keepServerUrl = true)
            toast("已退出登录")
            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
