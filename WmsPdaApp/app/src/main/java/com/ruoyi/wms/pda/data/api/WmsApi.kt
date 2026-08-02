package com.ruoyi.wms.pda.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * WMS 后端 Retrofit 接口。
 *
 * 注意：所有路径均为相对路径（不以 "/" 开头），由 ApiClient 中配置的 baseUrl 拼接为完整地址，
 * baseUrl 必须以 "/" 结尾，例如 http://192.168.1.100:8080/
 *
 * 认证：除 login / captchaImage 外，其余接口由 ApiClient 中的拦截器统一注入
 *      `Authorization: Bearer <token>` 请求头。
 */
interface WmsApi {

    // ------------------------------------------------------------------
    // 认证相关
    // ------------------------------------------------------------------

    /** 登录：POST /login，请求体为 JSON {username, password, code, uuid} */
    @POST("login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    /** 获取验证码：GET /captchaImage，返回 {code, uuid, img, captchaEnabled} */
    @GET("captchaImage")
    fun getCaptcha(): Call<CaptchaResponse>

    /** 获取登录用户信息：GET /getInfo，返回 {code, user, roles, permissions} */
    @GET("getInfo")
    fun getInfo(): Call<GetInfoResponse>

    /** 退出登录：POST /logout */
    @POST("logout")
    fun logout(): Call<ApiResponse<Any?>>

    // ------------------------------------------------------------------
    // 入库单 ReceiptOrder
    // ------------------------------------------------------------------

    /** 入库单分页列表：GET /wms/receiptOrder/list?orderNo=&orderStatus= */
    @GET("wms/receiptOrder/list")
    fun getReceiptOrderList(
        @Query("orderNo") orderNo: String?,
        @Query("orderStatus") orderStatus: String?
    ): Call<TableData<ReceiptOrder>>

    /** 获取入库单详情：GET /wms/receiptOrder/{id} */
    @GET("wms/receiptOrder/{id}")
    fun getReceiptOrder(@Path("id") id: Long): Call<ApiResponse<ReceiptOrderDetail>>

    /** 入库上架：POST /wms/receiptOrder/warehousing */
    @POST("wms/receiptOrder/warehousing")
    fun warehousing(@Body bo: ReceiptOrderBo): Call<ApiResponse<Any?>>

    // ------------------------------------------------------------------
    // 出库单 ShipmentOrder
    // ------------------------------------------------------------------

    /** 出库单分页列表：GET /wms/shipmentOrder/list?orderNo=&orderStatus= */
    @GET("wms/shipmentOrder/list")
    fun getShipmentOrderList(
        @Query("orderNo") orderNo: String?,
        @Query("orderStatus") orderStatus: String?
    ): Call<TableData<ShipmentOrder>>

    /** 获取出库单详情：GET /wms/shipmentOrder/{id} */
    @GET("wms/shipmentOrder/{id}")
    fun getShipmentOrder(@Path("id") id: Long): Call<ApiResponse<ShipmentOrderDetail>>

    /** 出库下架：POST /wms/shipmentOrder/shipment */
    @POST("wms/shipmentOrder/shipment")
    fun shipment(@Body bo: ShipmentOrderBo): Call<ApiResponse<Any?>>

    // ------------------------------------------------------------------
    // 库存 Inventory
    // ------------------------------------------------------------------

    /** 库存列表（不分页）：GET /wms/inventory/listNoPage?warehouseId= */
    @GET("wms/inventory/listNoPage")
    fun getInventoryList(@Query("warehouseId") warehouseId: Long?): Call<ApiResponse<List<Inventory>>>

    // ------------------------------------------------------------------
    // 库位 Location
    // ------------------------------------------------------------------

    /** 入库起始库位：GET /wms/location/receiptStart */
    @GET("wms/location/receiptStart")
    fun getReceiptStartLocations(): Call<ApiResponse<List<Location>>>

    /** 空库位：GET /wms/location/empty */
    @GET("wms/location/empty")
    fun getEmptyLocations(): Call<ApiResponse<List<Location>>>

    /** 占用库位：GET /wms/location/occupied */
    @GET("wms/location/occupied")
    fun getOccupiedLocations(): Call<ApiResponse<List<Location>>>

    /** 出库终点库位：GET /wms/location/shipmentEnd */
    @GET("wms/location/shipmentEnd")
    fun getShipmentEndLocations(): Call<ApiResponse<List<Location>>>

    /** 生成容器号：GET /wms/location/generateContainerNo */
    @GET("wms/location/generateContainerNo")
    fun generateContainerNo(): Call<ApiResponse<String>>

    /** 按库位编码获取容器号：GET /wms/location/containerNo?locationCode= */
    @GET("wms/location/containerNo")
    fun getContainerByLocation(@Query("locationCode") locationCode: String): Call<ApiResponse<String>>
}
