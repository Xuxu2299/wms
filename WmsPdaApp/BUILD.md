# WMS PDA 扫码 App 构建说明

## 项目概述

本项目是一个Android PDA扫码应用，用于WMS（仓储管理系统）的入库、出库、库存查询等操作。
支持PDA硬件扫码和摄像头扫码两种方式。

## 技术栈

- **语言**: Kotlin
- **最低SDK**: API 24 (Android 7.0)
- **目标SDK**: API 34 (Android 14)
- **构建工具**: Gradle 8.2 + AGP 8.2.0
- **网络框架**: Retrofit 2.9 + OkHttp 4.12
- **扫码库**: ZXing (zxing-android-embedded 4.3.0)
- **UI**: Material Design 3

## 项目结构

```
WmsPdaApp/
├── app/
│   ├── build.gradle.kts              # App模块构建配置
│   ├── proguard-rules.pro            # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml       # 清单文件
│       ├── java/com/ruoyi/wms/pda/
│       │   ├── WmsApp.kt             # Application类
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── ApiClient.kt  # Retrofit客户端
│       │   │   │   ├── ApiModels.kt  # 数据模型
│       │   │   │   └── WmsApi.kt     # API接口定义
│       │   │   └── prefs/
│       │   │       └── SessionManager.kt  # 会话管理
│       │   ├── scanner/
│       │   │   └── ScanReceiver.kt   # PDA硬件扫码接收器
│       │   ├── ui/
│       │   │   ├── base/BaseActivity.kt      # 基类Activity
│       │   │   ├── login/LoginActivity.kt    # 登录页
│       │   │   ├── main/MainActivity.kt      # 主菜单
│       │   │   ├── inbound/InboundActivity.kt # 入库扫码
│       │   │   ├── outbound/OutboundActivity.kt # 出库扫码
│       │   │   ├── inventory/InventoryActivity.kt # 库存查询
│       │   │   └── settings/SettingsActivity.kt   # 设置页
│       │   └── utils/ScanUtil.kt     # 摄像头扫码工具
│       └── res/
│           ├── layout/               # 布局文件
│           ├── values/               # 字符串、颜色、主题、尺寸
│           ├── drawable/             # 图标 drawable
│           └── mipmap-*/             # 启动器图标
├── build.gradle.kts                  # 根构建配置
├── settings.gradle.kts               # 项目设置
├── gradle.properties                  # Gradle属性
├── gradle/wrapper/                    # Gradle Wrapper
├── gradlew / gradlew.bat             # Gradle Wrapper脚本
└── BUILD.md                           # 本文件
```

## 构建步骤

### 方法一：使用 Android Studio（推荐）

1. 打开 Android Studio
2. 选择 `File > Open`，选择 `WmsPdaApp` 目录
3. 等待 Gradle 同步完成（会自动下载 gradle-wrapper.jar 和所有依赖）
4. 点击 `Build > Build Bundle(s) / APK(s) > Build APK(s)`
5. APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 方法二：使用命令行

前提条件：
- JDK 17 已安装并配置 JAVA_HOME
- Android SDK 已安装

```bash
# Windows
# 1. 如果没有 gradle-wrapper.jar，先安装 Gradle 并生成
gradle wrapper

# 2. 构建 Debug APK
gradlew.bat assembleDebug

# 3. 构建 Release APK
gradlew.bat assembleRelease
```

## 配置说明

### 服务器地址

App启动后在登录页输入服务器地址，格式：`http://IP:端口/`
例如：`http://192.168.1.100:8080/`

地址会保存在本地，可在设置页面修改。

### PDA硬件扫码适配

`ScanReceiver.kt` 支持多种PDA品牌的扫码广播：
- Honeywell/海讯
- Zebra/Motorola
- 新大陆/PT380
- 通用广播

如需适配特定PDA型号，修改 `ScanReceiver.kt` 中的 `SCAN_ACTIONS` 和 `SCAN_DATA_KEYS` 数组。

## 功能模块

| 模块 | 说明 |
|------|------|
| 登录 | 用户名密码登录，跳过验证码，适配PDA简化操作 |
| 入库扫码 | 自动生成单号和容器号，选择起点/目标库位，扫码添加商品 |
| 出库扫码 | 自动生成单号，选择有货库位自动带出容器号，扫码添加商品 |
| 库存查询 | 加载库存列表，按商品名称/编码实时搜索 |
| 设置 | 修改服务器地址，退出登录 |

## API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /login | POST | 登录 |
| /getInfo | GET | 获取用户信息 |
| /logout | POST | 退出登录 |
| /wms/receiptOrder/warehousing | POST | 入库上架 |
| /wms/shipmentOrder/shipment | POST | 出库下架 |
| /wms/inventory/listNoPage | GET | 库存列表 |
| /wms/location/receiptStart | GET | 入库起点库位(R1,R2) |
| /wms/location/empty | GET | 空库位(A1-A10) |
| /wms/location/occupied | GET | 有货库位 |
| /wms/location/shipmentEnd | GET | 出库终点库位(C1,C2) |
| /wms/location/generateContainerNo | GET | 生成容器号 |
| /wms/location/containerNo | GET | 按库位获取容器号 |
