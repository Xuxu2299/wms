package com.ruoyi.wms.pda.data.api

import com.google.gson.annotations.SerializedName

/**
 * 通用响应包装（单对象 / 简单结果）。
 * 后端 AjaxResult 结构：{ "code": 200, "msg": "操作成功", "data": {...} }
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String?,
    @SerializedName("data") val data: T?
) {
    /** RuoYi 约定 code == 200 表示成功 */
    val isSuccess: Boolean get() = code == 200
}

/**
 * 分页列表响应包装。
 * 后端 TableDataInfo 结构：{ "code": 200, "msg": "查询成功", "rows": [...], "total": 100 }
 */
data class TableData<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String?,
    @SerializedName("rows") val rows: List<T>? = null,
    @SerializedName("total") val total: Long? = null
) {
    val isSuccess: Boolean get() = code == 200
}

/**
 * /getInfo 接口响应：{ "code": 200, "user": {...}, "roles": [...], "permissions": [...] }
 */
data class GetInfoResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String?,
    @SerializedName("user") val user: UserInfo?,
    @SerializedName("roles") val roles: List<String>? = null,
    @SerializedName("permissions") val permissions: Set<String>? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================================================================
// 认证相关模型
// ======================================================================

/** 登录请求体：{username, password, code, uuid} */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("code") val code: String,
    @SerializedName("uuid") val uuid: String
)

/** 登录响应：{code, msg, token} */
data class LoginResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String?,
    @SerializedName("token") val token: String?
) {
    val isSuccess: Boolean get() = code == 200
}

/** 验证码响应：{code, uuid, img, captchaEnabled} */
data class CaptchaResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("img") val img: String?,
    @SerializedName("captchaEnabled") val captchaEnabled: Boolean
)

/** 登录用户信息（/getInfo 中的 user 对象） */
data class UserInfo(
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("deptId") val deptId: Long? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("nickName") val nickName: String? = null,
    @SerializedName("deptName") val deptName: String? = null,
    @SerializedName("phonenumber") val phonenumber: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("sex") val sex: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("remark") val remark: String? = null
)

// ======================================================================
// 入库单模型
// ======================================================================

/** 入库单列表 / 表头模型 */
data class ReceiptOrder(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("receiptOrderType") val receiptOrderType: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("supplierId") val supplierId: Long? = null,
    @SerializedName("supplierName") val supplierName: String? = null,
    @SerializedName("orderTime") val orderTime: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("remark") val remark: String? = null
)

/** 入库单明细行（detailList 子项） */
data class ReceiptOrderLine(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("receiptOrderId") val receiptOrderId: Long? = null,
    @SerializedName("materialId") val materialId: Long? = null,
    @SerializedName("materialCode") val materialCode: String? = null,
    @SerializedName("materialName") val materialName: String? = null,
    @SerializedName("materialSpec") val materialSpec: String? = null,
    @SerializedName("materialUnit") val materialUnit: String? = null,
    @SerializedName("batchNumber") val batchNumber: String? = null,
    @SerializedName("planQuantity") val planQuantity: Double? = null,
    @SerializedName("realQuantity") val realQuantity: Double? = null,
    @SerializedName("locationId") val locationId: Long? = null,
    @SerializedName("locationCode") val locationCode: String? = null,
    @SerializedName("containerNo") val containerNo: String? = null
)

/** 入库单详情（表头 + 明细行） */
data class ReceiptOrderDetail(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("receiptOrderType") val receiptOrderType: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("supplierId") val supplierId: Long? = null,
    @SerializedName("supplierName") val supplierName: String? = null,
    @SerializedName("orderTime") val orderTime: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("detailList") val detailList: List<ReceiptOrderLine>? = null
)

/** 入库上架请求体（warehousing） */
data class ReceiptOrderBo(
    @SerializedName("id") val id: Long,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("detailList") val detailList: List<ReceiptOrderLine>? = null
)

// ======================================================================
// 出库单模型
// ======================================================================

/** 出库单列表 / 表头模型 */
data class ShipmentOrder(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("shipmentOrderType") val shipmentOrderType: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("customerId") val customerId: Long? = null,
    @SerializedName("customerName") val customerName: String? = null,
    @SerializedName("orderTime") val orderTime: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("remark") val remark: String? = null
)

/** 出库单明细行（detailList 子项） */
data class ShipmentOrderLine(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("shipmentOrderId") val shipmentOrderId: Long? = null,
    @SerializedName("materialId") val materialId: Long? = null,
    @SerializedName("materialCode") val materialCode: String? = null,
    @SerializedName("materialName") val materialName: String? = null,
    @SerializedName("materialSpec") val materialSpec: String? = null,
    @SerializedName("materialUnit") val materialUnit: String? = null,
    @SerializedName("batchNumber") val batchNumber: String? = null,
    @SerializedName("planQuantity") val planQuantity: Double? = null,
    @SerializedName("realQuantity") val realQuantity: Double? = null,
    @SerializedName("locationId") val locationId: Long? = null,
    @SerializedName("locationCode") val locationCode: String? = null,
    @SerializedName("containerNo") val containerNo: String? = null
)

/** 出库单详情（表头 + 明细行） */
data class ShipmentOrderDetail(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("shipmentOrderType") val shipmentOrderType: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("customerId") val customerId: Long? = null,
    @SerializedName("customerName") val customerName: String? = null,
    @SerializedName("orderTime") val orderTime: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("detailList") val detailList: List<ShipmentOrderLine>? = null
)

/** 出库下架请求体（shipment） */
data class ShipmentOrderBo(
    @SerializedName("id") val id: Long,
    @SerializedName("orderNo") val orderNo: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("detailList") val detailList: List<ShipmentOrderLine>? = null
)

// ======================================================================
// 库存模型
// ======================================================================

/** 库存记录 */
data class Inventory(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("locationId") val locationId: Long? = null,
    @SerializedName("locationCode") val locationCode: String? = null,
    @SerializedName("materialId") val materialId: Long? = null,
    @SerializedName("materialCode") val materialCode: String? = null,
    @SerializedName("materialName") val materialName: String? = null,
    @SerializedName("materialSpec") val materialSpec: String? = null,
    @SerializedName("materialUnit") val materialUnit: String? = null,
    @SerializedName("batchNumber") val batchNumber: String? = null,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("containerNo") val containerNo: String? = null
)

// ======================================================================
// 库位模型
// ======================================================================

/** 库位 */
data class Location(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("locationCode") val locationCode: String? = null,
    @SerializedName("warehouseId") val warehouseId: Long? = null,
    @SerializedName("warehouseCode") val warehouseCode: String? = null,
    @SerializedName("warehouseName") val warehouseName: String? = null,
    @SerializedName("locationStatus") val locationStatus: String? = null,
    @SerializedName("containerNo") val containerNo: String? = null,
    @SerializedName("remark") val remark: String? = null
)
