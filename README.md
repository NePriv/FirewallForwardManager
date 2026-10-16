# FirewallForwardManager

Windows 端口转发（`netsh interface portproxy`）+ 防火墙放行的一体化图形界面工具。

输入「监听地址 / 监听端口 / 转发地址 / 转发端口」四个字段，即可一次性完成：

1. `netsh interface portproxy add` 添加端口转发规则
2. `netsh advfirewall firewall add rule` 放行对应入站端口（否则转发规则存在但外部连不上）
3. 在列表中实时查看已有规则，并一键删除转发 + 对应的防火墙规则

## 环境要求

- Windows 10 / 11 或 Windows Server
- JDK 25（`pom.xml` 中 `maven.compiler.release=25`）
- 需要管理员权限：程序启动时会检测，并通过 UAC 自动以管理员身份重启

## 运行方式

在 IDE（IntelliJ IDEA / VS Code）中直接运行 `org.paperweight.ffm.Main`：

- 若当前不是管理员，程序会先弹窗询问：
  - **以管理员身份重启（推荐）**：触发 UAC 后以管理员身份重新打开
  - **只读模式查看**：可刷新、查看规则、复制命令，但不能增删
  - **退出**
- 开发调试时可加 `-Dffm.noElevate=true` 跳过提权弹窗（此时若不是管理员则进入只读模式）；
  注意提权后是一个新的 JVM 进程，**IDE 中的断点不会命中**，调试界面请从管理员终端启动 IDE。
- 排查乱码时可用 `-Dffm.charset=GBK` 强制指定回退编码。

也可以用 Maven 直接运行（需在管理员终端中执行）：

```
mvn -q compile
mvn -q exec:java
```

## 可视化编辑界面（IntelliJ IDEA）

界面布局保存在 `src/main/java/org/paperweight/ffm/ui/MainFrame.form`，在 IDEA 中双击该文件即可拖拽控件、
在组件树里调整层级、用属性面板改文案与提示（右键表单可选 Preview 预览）。首次使用需在
`Settings → Plugins → Marketplace` 安装官方 **Swing UI Designer** 插件。

必须把 `Settings → Editor → GUI Designer → Generate GUI into` 设为 **Java source code**：
生成代码会写进 `MainFrame.java` 的 `$$$setupUI$$$()` 方法中（IDE 生成，不要手改），
只有这种模式才能让命令行的 `mvn` 构建正常工作；默认的 `Binary class files` 模式只在 IDEA 自己的编译器下生效，
用 Maven 构建会因缺少 setupUI 代码而在运行时抛 `NullPointerException`。

维护约定：

- 在表单中放置控件时填写 field name，且要与 `MainFrame.java` 中同名的字段声明一一对应（当前 23 个绑定字段）；
- 布局只用标准 Swing 布局（BorderLayout / FlowLayout），所以不依赖 `com.intellij:forms_rt`；
- 与布局无关的部分留在代码里：数据模型、表格行为、边框见 `configureComponents()`，事件绑定见 `wireActions()`。

## 界面说明

| 区域 | 说明 |
| --- | --- |
| 添加端口转发 | 转发类型（IPv4→IPv4、IPv6→IPv6、IPv6→IPv4、IPv4→IPv6）、协议（TCP/UDP）、监听地址（可编辑下拉，`0.0.0.0` / `::` 表示监听所有地址）、监听端口、转发地址、转发端口 |
| 复选项 | 自动添加防火墙入站放行规则（默认勾选）；入站规则仅允许来自转发目标地址的流量（可选，更严格） |
| 工具栏 | 刷新列表、删除选中、清理孤立防火墙规则、清空全部转发（二次确认） |
| 表格 | 转发类型 / 协议 / 监听地址 / 监听端口 / 转发地址 / 转发端口 / 防火墙入站放行状态 |
| 日志 | 实际执行的 netsh 命令与原始输出，可复制后手工执行排障 |

## 实现要点

- **提权模型**：启动时整体提权。通过 `whoami /groups` 中的强制完整性级别 SID（`S-1-16-12288`）判断，
  与系统显示语言无关；提权用 PowerShell `Start-Process -Verb RunAs` 拉起等价的 java 命令行，
  子进程写出标记文件以确认提权是否真的成功（识别用户点了“否”的情况）。
- **输出编码**：Windows 各工具的编码并不统一（实测同一台机器上 `netsh` 输出 UTF-8，而 `whoami` 输出控制台代码页 GBK），
  因此程序对每条命令的输出先做 UTF-8 严格校验，合法则按 UTF-8 解码，否则回退到用 `chcp` 探测到的控制台字符集，避免中文乱码。
- **解析健壮性**：`portproxy show` 的表头随系统语言变化，因此分别执行 `show v4tov4 / v6tov6 / v6tov4 / v4tov6`
  并按数据行形态（地址 端口 地址 端口）解析，不依赖任何本地化文本。
- **规则命名**：防火墙规则统一命名为 `FFM-IN-<协议>-<监听端口>-<地址后缀>`（IPv4 直接用点分十进制，
  IPv6 用地址的短哈希，避免规则名中出现冒号），以便随转发规则一起删除并清理残留。
- **原子性**：添加转发时若防火墙规则创建失败，会自动回滚刚添加的转发规则；
  删除转发时对 TCP 与 UDP 各尝试一次（`portproxy` 的列表不显示协议）。
- **IP Helper**：转发依赖 `iphlpsvc` 服务，启动时会检查并尝试启动它。

## 测试

```
mvn test
```

单元测试覆盖：`portproxy show` 输出解析（中英文表头、空表、IPv6、异常行）、防火墙规则名提取、
netsh 命令行等价文本、地址/端口校验、代码页映射、提权相关的引号与参数过滤逻辑。

## 手工验收（需管理员）

```
# 1. 在界面中添加：监听 192.168.3.6:2405 → 转发 112.11.36.130:10010（TCP，勾选自动放行）
netsh interface portproxy show all
netsh advfirewall firewall show rule name=FFM-IN-TCP-2405-192.168.3.6

# 2. 从另一台机器验证连通性
Test-NetConnection 192.168.3.6 -Port 2405

# 3. 在界面中删除该规则，确认转发与防火墙规则同时消失
netsh interface portproxy show all
```
