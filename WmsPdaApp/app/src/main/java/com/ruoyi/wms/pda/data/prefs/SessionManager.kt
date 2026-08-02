package com.ruoyi.wms.pda.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.ruoyi.wms.pda.WmsApp

/**
 * SharedPreferences 轻量封装，集中管理本地持久化的会话信息。
 *
 * 持久化字段：
 * - serverUrl    服务器地址（如 http://192.168.1.100:8080/）
 * - token        登录令牌（Bearer Token）
 * - username     登录用户名
 * - userId       登录用户 ID
 * - warehouseId  当前选择的仓库 ID
 *
 * 通过 [WmsApp.instance] 获取 Application Context，故需在 WmsApp 初始化后使用。
 */
object SessionManager {

    private const val PREF_NAME = "wms_pda_prefs"

    private const val KEY_SERVER_URL = "serverUrl"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "userId"
    private const val KEY_WAREHOUSE_ID = "warehouseId"

    private val prefs: SharedPreferences
        get() = WmsApp.instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------
    // serverUrl
    // ------------------------------------------------------------------

    /** 服务器地址（建议以 "/" 结尾，如 http://192.168.1.100:8080/） */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    // ------------------------------------------------------------------
    // token
    // ------------------------------------------------------------------

    /** 登录令牌 */
    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** 是否已登录（token 非空即视为已登录） */
    val isLoggedIn: Boolean
        get() = token.isNotEmpty()

    // ------------------------------------------------------------------
    // username
    // ------------------------------------------------------------------

    /** 当前登录用户名 */
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    // ------------------------------------------------------------------
    // userId
    // ------------------------------------------------------------------

    /** 当前登录用户 ID */
    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    // ------------------------------------------------------------------
    // warehouseId
    // ------------------------------------------------------------------

    /** 当前选择的仓库 ID（0 表示未选择） */
    var warehouseId: Long
        get() = prefs.getLong(KEY_WAREHOUSE_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_WAREHOUSE_ID, value).apply()

    // ------------------------------------------------------------------
    // 清理
    // ------------------------------------------------------------------

    /**
     * 清除所有会话信息（退出登录时调用）。
     * 注意：默认不清除 serverUrl，以便下次自动填充服务器地址；
     * 如需一并清除服务器地址，请传入 [keepServerUrl] = false。
     */
    fun clear(keepServerUrl: Boolean = true) {
        val serverUrlBackup = serverUrl
        prefs.edit().clear().apply()
        if (keepServerUrl) {
            serverUrl = serverUrlBackup
        }
    }
}
