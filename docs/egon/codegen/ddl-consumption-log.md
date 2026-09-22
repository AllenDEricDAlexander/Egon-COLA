# Codegen classpath SQL journal

Skill 驱动的代码生成在这里记录用过的 `src/main/resources/db/` SQL。每一条只追加。它不是 `ddl_history`，也不表示这些 SQL 已经在 PostgreSQL 上执行。

还没有生成记录。

下一次追加使用这个形状：

- 二级标题是 Asia/Shanghai 时间。
- 列出 Profile、仓库内相对输出根、Through version（最高版本；没有 db 脚本则为 none）、Command（plan 或 apply）。
- 每个脚本再列版本号、仓库内相对路径、文件字节的 SHA-256，以及完整 SQL。
