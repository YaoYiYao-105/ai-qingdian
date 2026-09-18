# 训练数据字典与转换流程

## 1. 存储位置

| 路径 | 内容 | 是否入库 |
| --- | --- | --- |
| `backend/ai_resources/images/YYYY/MM/DD/<sha256>.jpg` | 纠错样本原图，按内容哈希命名 | 否（`.gitignore`） |
| `backend/ai_resources/corrections.jsonl` | 每行一条纠错会话 | 否（`.gitignore`） |
| `docs/samples/corrections.sample.jsonl` | 脱敏样例 | 是 |

存储根目录由 `correction.storage-dir` 配置（默认 `ai_resources`），生产环境建议通过环境变量 `CORRECTION_STORAGE_DIR` 指向独立数据盘或对象存储挂载点。

## 2. `corrections.jsonl` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `session_id` | string | 一次纠错会话的 UUID（无横线） |
| `created_at` | string | ISO-8601 带时区时间戳 |
| `image.file` | string | 相对存储根目录的图片路径 |
| `image.sha256` | string | 图片内容哈希，用于去重与数据一致性校验 |
| `model` | string | 产生原始输出的模型 ID，用于区分数据来源版本 |
| `raw_model_text` | string | 模型返回的原始文本（含多余文字时原样保留） |
| `items[]` | array | 识别结果全量快照，见下表 |
| `corrections[]` | array | 仅包含被人工修改或标记「看不清」的行 |

### `items[]` 字段

| 字段 | 说明 |
| --- | --- |
| `row` | 页面上从 1 开始的行号 |
| `recognized_name` | 模型识别名称（归一化前） |
| `selected_name` | 前端最终选定的名称（可能仍等于识别名称） |
| `model_raw_name` | 模型最原始叫法，用于分析归一化损失 |
| `fill_ratio` | 剩余量占比 0–100，`null` 表示模型未给出 |
| `confidence` | `high` / `medium` / `low` |
| `description` | 位置或外观补充描述 |
| `x/y/width/height` | 预留字段：接入检测模型后回填原图坐标 |

### `corrections[]` 字段

| 字段 | 说明 |
| --- | --- |
| `row` | 对应 `items[].row` |
| `recognized_name` | 模型原名 |
| `corrected_name` | 人工修正后的官方名；标记为 `unclear` 时为空串 |
| `unclear` | `true` 表示人工判定「看不清」，属于待挖掘难例 |
| `fill_ratio` / `confidence` / `description` | 冗余保存，便于脱离 `items` 单独消费 |

## 3. 转换成训练集

### 3.1 监督信号映射

| 训练任务 | 输入 | 标签来源 |
| --- | --- | --- |
| 托盘检测 / 分割 | 原图 | 当前为空，需用 OpenCV 结果预标注 + 人工修正 |
| 菜品细粒度分类 | 托盘裁切图 | `corrections[].corrected_name`（并合并 `selected_name` 与清单别名） |
| 剩余量回归 | 托盘裁切图 | 需补人工占比标注；`items[].fill_ratio` 仅作为弱标签/预标注 |
| 多模态 SFT / DPO | 原图 + 指令 | 首选答案 = 人工修正后的完整 JSON；DPO 偏好对 = (修正后, 模型原始) |
| 难例挖掘 | 原图 | `confidence=low` 或 `unclear=true` |

### 3.2 导出脚本示例

```python
import json
from pathlib import Path

root = Path("backend/ai_resources")
rows = [json.loads(line) for line in (root / "corrections.jsonl").read_text("utf-8").splitlines() if line.strip()]

# 1) 菜品分类数据集：每行 = (裁切图, 官方名)
cls_records = []
for rec in rows:
    fixed = {c["row"]: c.get("corrected_name") or c["recognized_name"]
             for c in rec["corrections"] if not c.get("unclear")}
    for item in rec["items"]:
        name = fixed.get(item["row"], item["selected_name"] or item["recognized_name"])
        if not name:
            continue
        cls_records.append({"image": rec["image"]["file"], "label": name,
                            "confidence": item["confidence"], "row": item["row"]})

# 2) 难例清单：优先送人工复标
hard_cases = [r for r in rows
              if any(i["confidence"] == "low" for i in r["items"])
              or any(c.get("unclear") for c in r["corrections"])]

print("分类样本:", len(cls_records), "难例会话:", len(hard_cases))
```

### 3.3 数据集切分原则

- **按门店 + 日期切分**：同一门店同一天的样本只能出现在一个集合中，避免相邻帧泄漏导致指标虚高。
- 训练 : 验证 : 测试建议 `8 : 1 : 1`，其中测试集需覆盖所有在售品类与至少 3 种典型光照条件。
- 测试集一旦冻结不得回流训练，新增数据只进训练/验证集。

### 3.4 版本管理

- 数据与模型用 **DVC** 管理，`corrections.jsonl` + 图片目录作为一次数据快照打 tag（如 `data-v2026.03`）。
- 每次训练记录 `数据版本 + 代码 commit + 超参 + 指标` 到 MLflow，保证结果可复现、可回滚。
