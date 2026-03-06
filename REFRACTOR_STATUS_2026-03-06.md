# Refactor Status — 2026-03-06

## Date / Branch / HEAD

| Item | Value |
|------|-------|
| Date | 2026-03-07 (JST) |
| Branch | `master` |
| HEAD hash | `d91a064904481b622719f9584c9a98246c034326` |
| HEAD date | 2026-03-07 03:35:26 +0900 |
| HEAD message | `Merge pull request #1 from PangYanbo/integrate/merge-dev-to-master` |

### Recent commits (last 5)
```
d91a064  2026-03-07  Merge pull request #1 from PangYanbo/integrate/merge-dev-to-master
f4e1560  2026-03-04  merge: integrate pseudo-pflow-v3-dev-hms into master (unrelated histories)
c5f606b  2026-03-03  refactor: migrate commons-lang 2.6 → lang3 3.12.0, commons-math 2.2 → math3 3.6.1
72f5b9d  2026-03-03  docs: update CLAUDE.md and README for pflowlib sources and credentials
d14ab2d  2026-03-03  build: pin GeoTools to stable 26.5, fix java.version, upgrade plugins
```

---

## Completed（已完成）

### (4) pflowlib 源码集成
- 306 个 Java 文件从 `lib/pflowlib.jar` 反编译后迁入 `src/jp/ac/ut/csis/pflow/`
- CFR 0.152 反编译产物（try-with-resources 包装、未初始化变量、标签块等）已全部修复
- `src/main/resources/log4j2.xml` 从 JAR 内嵌资源提取，放置于 Maven 资源目录
- `pom.xml` 新增 `<sourceDirectory>src</sourceDirectory>`
- `mvn compile` → BUILD SUCCESS（已验证）

### (5) 死代码清理 & 安全
- `config.properties` 明文凭证已替换为环境变量占位符（`${PFLOW_API_USER}` / `${PFLOW_API_PASS}`）
- 已删除：`src/pseudo/gen/RoadRouteTest.java`（含硬编码凭证）
- 已删除：`src/pseudo/gen/TransportTest.java`、`src/test/test.java`
- 已删除：`src/sim/sim3/`（22 个文件，依赖旧 commons-lang2，已被 sim4 取代）
- 已删除：`src/particle/IParticle.java`、`src/particle/ParticleFilter.java`

### (2) 构建可复现性
- `geotools.version`: `26-SNAPSHOT` → `26.5`（稳定版）
- `java.version`: `1.8` → `11`（与编译器插件一致）
- 移除 OSGeo snapshot 仓库
- `maven-compiler-plugin`: `3.0` → `3.11.0`
- `maven-assembly-plugin`: `3.1.0` → `3.6.0`

### (3) 依赖清理
- `commons-lang:2.6` → `commons-lang3:3.12.0`（84 个文件 import 路径已更新）
- `commons-math:2.2` → `commons-math3:3.6.1`（6 个文件 import 路径已更新）
- `org.json`: `20200518` → `20231013`
- `gt-swing` 依赖已移除（GUI 模块，pipeline 中未使用）
- `otp:1.4.0` 改为 `<scope>provided</scope>`
- JTS 命名空间迁移：`com.vividsolutions.jts.*` → `org.locationtech.jts.*`（pflowlib 源码内）

### (1) 分支整合
- `integrate/merge-dev-to-master` 分支从 `master` 创建，以 `--allow-unrelated-histories` 方式合并 `pseudo-pflow-v3-dev-hms`
- 所有 add/add 冲突取 dev 版本（完整、可构建的代码库）
- 整合分支已推送并通过 PR #1 合并入 `master`
- 所有原始分支均未删除

---

## Verified by Commands（关键命令与结果）

| 命令 | 结果 |
|------|------|
| `find src/jp/ac/ut/csis/pflow -name "*.java" \| wc -l` | **306** |
| `ls src/main/resources/log4j2.xml` | **存在** |
| `grep "sourceDirectory" pom.xml` | **`<sourceDirectory>src</sourceDirectory>`（第 132 行）** |
| `grep "pflowlib" pom.xml` | 仅注释行（`<!-- pflowlib source dependencies... -->`），无 JAR 依赖声明 |
| `grep "api\." config.properties` | `api.userID=${PFLOW_API_USER}`，`api.password=${PFLOW_API_PASS}`（无明文） |
| `[ -e src/sim/sim3 ]` | **GONE** |
| `[ -e src/pseudo/gen/RoadRouteTest.java ]` | **GONE** |
| `[ -e src/pseudo/gen/TransportTest.java ]` | **GONE** |
| `[ -e src/test/test.java ]` | **GONE** |
| `grep "geotools.version\|java.version" pom.xml` | `26.5` / `11` |

---

## Known Risks / TODO（已知风险与待办）

### 残留问题（需后续处理）
1. **`.idea/libraries/pflowlib.xml`** 在 master 合并后重新出现（master 分支历史中含此文件）。该文件让 IntelliJ 仍将 `lib/pflowlib.jar` 列为 library，但 Maven 构建**不受影响**。建议删除并提交。
2. **`src/particle/`** 目录仍存在（空目录，无 `.java` 文件）。可随时清理。
3. **Git 历史含明文凭证**：`api.password=Pyb-37167209` 存在于旧 commit 中（本次仅清理工作区，未做历史重写）。若仓库为私有可暂缓；若为公开仓库，建议用 `git filter-repo` / BFG 做一次历史清洗，并轮换 API 密码。

### 其它机器上的分支
- 其它服务器/机器上可能存在尚未推送至 GitHub 的本地分支（如 `hms-server2` 的服务器本地变体）。
- 在这些分支 push 并比对之前，**不要删除任何分支**（参见 CLAUDE.md 中的"Future branch intake SOP"）。
- 待办：各机器上执行 `git checkout -b server/<hostname>/<YYYYMMDD>` → push → 在此机器 `git fetch` → `git range-diff` 比对后再决定是否 cherry-pick。

### 构建环境
- 本机需使用 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn compile`（系统默认 Java 非 21）。
- Maven 版本要求 3.6.3（≥3.8 不兼容，见 CLAUDE.md）。
- 未添加 Maven Wrapper（`mvnw`），团队环境一致性依赖手动约定。

---

## Next Steps（下一步建议）

1. **清理残留 IDE 文件**（可选，低优先级）：
   ```bash
   git rm .idea/libraries/pflowlib.xml
   git rm -r src/particle   # 空目录
   git commit -m "chore: remove stale pflowlib IDE library and empty particle dir"
   ```

2. **验证完整构建**：
   ```bash
   JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn clean package -DskipTests
   # 预期：BUILD SUCCESS，生成 target/DSPFlow-0.0.1-SNAPSHOT-jar-with-dependencies.jar
   ```

3. **Pipeline smoke test**（小数据端到端验证）：
   - 更新 `config.properties` 中的 `root` / `inputDir` 路径为本机实际路径
   - 设置环境变量：`export PFLOW_API_USER=... PFLOW_API_PASS=...`
   - 跑 Step 1：`mvn exec:java -Dexec.mainClass="pseudo.pre.PersonGenerator"` on a single prefecture

4. **Git 历史清洗**（如仓库为公开）：
   - 使用 `git filter-repo --path src/pseudo/gen/RoadRouteTest.java --invert-paths` 等移除含凭证的历史提交
   - 完成后轮换 CSIS WebAPI 密码

5. **其它机器分支整合**：
   - 按 CLAUDE.md "Future branch intake SOP" 操作，逐机器 push → range-diff → cherry-pick

6. **（可选）添加 Maven Wrapper**：
   ```bash
   mvn wrapper:wrapper -Dmaven=3.6.3
   git add .mvn/ mvnw mvnw.cmd
   git commit -m "build: add Maven wrapper locked to 3.6.3"
   ```
