# 轻课 Android

一个可离线使用的 Android 课表应用，支持昆明理工大学教务课表导入、多课表、桌面小组件和上课提醒。当前版本 **0.4.1 体验版**，适用于 Android 8.0 及以上。

[下载安装包](https://github.com/jack242-vf/qingke-android/releases/latest) · [使用指南](docs/GUIDE.md) · [更新记录](CHANGELOG.md) · [构建说明](docs/BUILD.md)

## 安装

在 Releases 下载 `qingke-0.4.1-debug.apk`，传到安卓手机并通过系统安装器安装。更新前建议导出课表备份。该安装包使用开发体验签名，未上架应用商店。

## 功能

- 每日与整周课表、离线存储、最多 10 份独立课表。
- 课程编辑、多时段、单双周与不连续周次、备注、单周调课与停课。
- 昆工原生登录窗口：本人登录后，打开课表并点击提取，预览确认后导入。
- JSON 文件导入与备份导出；各课表独立的主题、字体、周末显示和视图设置。
- 今日课表桌面小组件、提前上课提醒及通知权限设置。

课表保存在各自手机本地，应用不提供云端账号或多人数据同步。学校登录由学校页面处理，应用不提取账号、密码或登录 Cookie；仅在点击提取时读取课表内容。

## 构建与验证

需要 Java 17+、Android SDK 35；仓库附带 Gradle 8.11.1 Wrapper。

```sh
./gradlew :app:assembleDebug
```

Windows 使用 `gradlew.bat :app:assembleDebug`。测试需要 Node.js 22+：

```powershell
./tools/test.ps1
```

测试脚本可在 PowerShell 7 的 Windows/Linux 环境运行。详细环境配置、Windows 最小构建和签名说明见 [BUILD.md](docs/BUILD.md)。GitHub Actions 自动运行测试并构建 APK；CI 安装包签名与 Releases 可能不同。

## 当前限制

0.4.1 已通过 24 项 JavaScript 测试、40 项导航断言、27 项课表与提醒断言，以及本地 APK 构建和 v2/v3 签名校验。学校真实登录、不同手机的后台提醒和桌面小组件仍需真机验证，不能保证兼容所有系统。学校页面改版可能需要更新导入适配。

这是独立实现的课表应用，不包含 WakeUp 的品牌、代码或素材。签名私钥、个人课表和本机配置不进入仓库。

## 项目结构

- `app/src/main/java`：Android 容器、学校导航、提醒与小组件。
- `app/src/main/assets/www`：离线课表界面与解析逻辑。
- `tests`：JavaScript 和 Java 测试。
- `tools`：检查、构建与签名工具源码。
- `docs`：使用、构建和发布文档。
