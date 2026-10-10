## Skills Setup

项目中的 Skills 默认维护在 `.agents/skills` 目录。

### 1. 从指定目录复制 Skills 到当前项目

将其他项目或指定目录中的 `.agents/skills` 内容复制到当前项目：

```bash
mkdir -p .agents/skills
cp -R /path/to/source-project/.agents/skills/. .agents/skills/
```

例如：

```bash
mkdir -p .agents/skills
cp -R ~/workspace/source-project/.agents/skills/. .agents/skills/
```

如果希望复制时直接覆盖当前目录中的同名文件：

```bash
mkdir -p .agents/skills && cp -Rf /path/to/source-project/.agents/skills/. .agents/skills/
```

目录关系：

```text
/path/to/source-project/
└── .agents/
    └── skills/
        ├── skill-a/
        └── skill-b/

        ↓ copy

current-project/
└── .agents/
    └── skills/
        ├── skill-a/
        └── skill-b/
```

### 2. 从当前 `.agents/skills` 复制到 Claude

将当前项目的 `.agents/skills` 复制到 `.claude/skills`：

```bash
mkdir -p .claude/skills
cp -R .agents/skills/. .claude/skills/
```

如果希望直接覆盖同名文件：

```bash
mkdir -p .claude/skills && cp -Rf .agents/skills/. .claude/skills/
```

目录关系：

```text
current-project/
├── .agents/
│   └── skills/
│       ├── skill-a/
│       └── skill-b/
│
└── .claude/
    └── skills/
        ├── skill-a/
        └── skill-b/
```

### 常用命令

从指定项目同步到当前 `.agents/skills`：

```bash
mkdir -p .agents/skills && cp -Rf /path/to/source-project/.agents/skills/. .agents/skills/
```

从当前 `.agents/skills` 同步到 `.claude/skills`：

```bash
mkdir -p .claude/skills && cp -Rf .agents/skills/. .claude/skills/
```

依次执行两个步骤：

```bash
mkdir -p .agents/skills .claude/skills \
  && cp -Rf /path/to/source-project/.agents/skills/. .agents/skills/ \
  && cp -Rf .agents/skills/. .claude/skills/
```
