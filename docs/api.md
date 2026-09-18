# 接口文档

Base URL：`http://localhost:8080`（生产环境请置于 HTTPS 网关之后）

## 约定

- 统一响应体：`{ "code": number, "message": string, "data": object|null }`
- 字段命名：`snake_case`（由 `spring.jackson.property-naming-strategy=SNAKE_CASE` 保证）
- 图片上传：`multipart/form-data`，字段名固定为 `file`
- 支持格式：`jpg/jpeg/png/bmp/gif`，单文件 ≤ 10MB

| code | 含义 | HTTP 状态 |
| --- | --- | --- |
| `0` | 成功 | 200 |
| `1` | 服务内部异常（含模型调用失败、JSON 解析失败） | 200/500 |
| `2` | 参数错误（空文件、格式不支持、报文非法） | 200 |
| `3` | 文件过大 | 200 |

> 业务错误统一通过 `code` 表达，前端只需判断 `code === 0`，避免依赖 HTTP 状态分支。

---

## 1. 获取商品清单

`GET /api/products`

**响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "name": "荣昌卤鹅", "aliases": null },
    { "name": "云丝", "aliases": ["卤豆腐丝", "干豆腐丝", "凉拌豆腐丝"] }
  ]
}
```

**说明**：清单在服务启动时从 `classpath:product-catalog.json` 加载；识别时会被拼进系统提示词，同时用于名称归一化与双拼拆分。

---

## 2. 识别展示柜照片

`POST /api/analyze`

**请求**（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 展示柜照片 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total_trays` | int | 识别到的托盘数（双拼拆分后计数） |
| `stock_score` | int | 平均剩余占比 0–100，`-1` 表示无法计算 |
| `stock_level` | string | `充足 / 一般 / 较少 / 严重不足 / 未知` |
| `order_reasonableness` | string | `合理 / 基本合理 / 偏低 / 明显偏低 / 偏高 / 未知` |
| `reason` | string | 结论文字（含托盘数、均分、补货盘数、未匹配数、模型备注） |
| `restock_suggestions` | array | 剩余 `较少/严重不足` 的托盘列表（元素结构同 `items`） |
| `items` | array | 全部托盘明细 |
| `raw` | string | 模型原始返回文本（便于排障，前端在失败态展示） |

**`items[]` 字段**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | string | 归一化后的商品名；未匹配清单时为模型原始描述 |
| `original_name` | string\|null | 归一化前的模型叫法（与 `name` 不同时才有值） |
| `model_raw_name` | string | 模型最原始叫法，用于纠错数据 |
| `fill_ratio` | int | 剩余量占比 0–100，`-1` 表示模型未给出 |
| `level` | string | 该托盘充足度等级 |
| `confidence` | string | `high / medium / low / unknown` |
| `matched` | bool | 是否命中商品清单，`false` 需人工确认 |
| `source` | string | `model`=模型直接识别；`split`=双拼自动拆分 |
| `description` | string | 位置或外观补充 |

**示例**

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "file=@shelf.jpg"
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total_trays": 17,
    "stock_score": 82,
    "stock_level": "充足",
    "order_reasonableness": "合理",
    "reason": "共识别 17 个陈列餐盘（其中 1 个双拼已拆分），平均剩余占比约 82%，其中 2 盘剩余偏少。整体库存充足，本次订货量合理。",
    "restock_suggestions": [
      { "name": "木耳", "fill_ratio": 40, "level": "一般", "confidence": "high", "matched": true, "source": "model" }
    ],
    "items": [
      { "name": "麻辣毛豆", "original_name": null, "model_raw_name": "麻辣毛豆", "fill_ratio": 90,
        "level": "充足", "confidence": "high", "matched": true, "source": "model", "description": "上层最左侧托盘" }
    ],
    "raw": "..."
  }
}
```

**解析失败示例**

```json
{ "code": 1, "message": "模型返回内容无法解析为 JSON，请查看原始文本排查。原始内容：{...}", "data": { "raw": "模型原始文本" } }
```

---

## 3. 保存人工纠错样本

`POST /api/corrections`

**请求**（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 与识别时相同的原图 |
| `json` | string | 是 | 纠错报文 JSON 字符串 |

**`json` 报文结构**

| 字段 | 说明 |
| --- | --- |
| `raw_model_text` | 模型原始返回文本 |
| `items[]` | 全量识别结果快照 |
| `corrections[]` | 仅被修改或标记「看不清」的行 |

**校验规则**

- `items` 不能为空；每行的 `row` 必须存在且唯一。
- `corrections[].corrected_name` 与 `unclear=true` 不能同时缺省（既没改也没标看不清的行前端不会提交）。
- `fill_ratio` 若存在必须在 `0–100`。

**响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "session_id": "a4d64e330d1f4c6fa9d88b926aa0e810",
    "image_file": "images/2026/09/07/e341e4aa...d1.jpg",
    "corrections_count": 1
  }
}
```

**说明**

- 图片按 SHA-256 去重：同一张图重复提交不会重复占用磁盘。
- 写入 `corrections.jsonl` 使用 `ReentrantLock` + 追加写，保证并发安全。

---

## 4. OpenCV 路线：托盘检测与占比

`POST /api/analyze-cv`

**请求**（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 展示柜照片 |
| `grid` | string | 否 | 等分网格模式，如 `3x5`（3 行 5 列）；缺省自动检测托盘 |

**响应 `data`**

| 字段 | 说明 |
| --- | --- |
| `tray_count` | 检测到的托盘数 |
| `trays[]` | 每个托盘的 `index / x / y / width / height / fill_ratio / level / food_pixels / empty_pixels / unknown_pixels` |
| `original_width` / `original_height` | 原图尺寸（`trays[]` 坐标已换算回原图坐标系） |
| `processed_width` / `processed_height` | 实际参与计算的处理图尺寸 |
| `strategy` | `auto`=自动检测；`grid`=等分网格 |
| `contour_count` / `candidate_count` | 轮廓数与候选框数，用于调参诊断 |
| `annotated_base64` | 标注后的 JPEG（base64，不含 `data:` 前缀） |

**算法参数**（`application.yml` → `cv.*`）：最长边、模糊核、自适应阈值 `blockSize/C`、形态学核、最小/最大面积、贴边保护、边框采样比例、Lab 颜色距离阈值、高光阈值。

调试页面：`GET /cv-test.html`
