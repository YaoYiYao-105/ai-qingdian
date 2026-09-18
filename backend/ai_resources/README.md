# ai_resources（运行时数据目录）

该目录用于存放识别与纠错产生的运行时数据，**默认不纳入版本库**（见根目录 `.gitignore`）。

```text
ai_resources/
├── images/YYYY/MM/DD/<sha256>.jpg    # 纠错样本原图，按内容哈希命名（自动去重）
└── corrections.jsonl                  # 每行一条纠错会话（模型输出 + 人工修正）
```

- 存储根目录由 `correction.storage-dir` 配置，可用环境变量 `CORRECTION_STORAGE_DIR` 覆盖。
- 生产环境建议把该目录挂载到独立数据盘或对象存储，并配置备份策略。
- 数据格式与训练集导出方式见 [`../../docs/training-data.md`](../../docs/training-data.md)。
- 对外演示 / 开源时请使用脱敏样本（参见 `docs/samples/corrections.sample.jsonl`），不要提交真实门店照片。

本文件所在目录会在首次保存纠错数据时自动创建子目录。
