# 发布版本

1. 在 `app/build.gradle` 增加 `versionCode` 并更新 `versionName`；同步 `package.json`、应用内关于文字与 UA 版本。
2. 更新 `CHANGELOG.md` 和 `docs/releases/` 发布说明。
3. 运行完整检查和 APK 构建。在手机验证学校登录、导入、升级、通知与桌面组件。
4. 使用固定、妥善保管的签名生成发布 APK。不要提交 `.p12`、`.jks`、`.keystore`、密码或学校登录数据。
5. 对 APK 生成 SHA-256 清单，在 GitHub 创建对应 `vX.Y.Z` 标签与 Release，上传 APK 和清单。

0.4.1 的说明见 [v0.4.1](releases/v0.4.1.md)。已知尚未真机验证的功能必须在 Release 说明中保留，不能用编译成功替代实际手机验证。

CI 只自动检查与构建，不自动发行安装包，避免临时签名导致升级不兼容。不要把维护者签名私钥直接上传到仓库。
