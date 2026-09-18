# 前端模块说明（uni-app / Vue 3）

本目录是从完整 uni-app 工程中抽取的 **AI 商品识别模块**，可直接复制到任意 uni-app 项目中使用。

## 文件清单

```text
frontend/
├── pages/aiqingdian/
│   ├── index.vue           # 页面主体：选图 → 识别 → 结果表格 → 纠错保存
│   ├── SearchSelect.vue    # 可搜索的商品名称下拉（带「已修改」高亮）
│   └── correctionUtils.js  # 纠错 diff / 序列化（纯函数）
└── api/
    └── aiQingdian.js       # 后端接口封装（uni.request / uni.uploadFile）
```

## 集成步骤

1. 复制 `pages/aiqingdian/` 到你的 uni-app 项目 `pages/` 目录；
2. 复制 `api/aiQingdian.js` 到项目 `api/` 目录；
3. 在 `pages.json` 的 `pages` 数组中注册页面：

```json
{
  "path": "pages/aiqingdian/index",
  "style": {
    "navigationBarTitleText": "AI 商品识别",
    "navigationStyle": "custom"
  }
}
```

4. 在项目根目录 `.env`（或对应环境文件）中配置后端地址：

```bash
VUE_APP_AIQingdianBaseUrl=https://your-backend.example.com
```

> uni-app 在构建时会**静态替换** `process.env.VUE_APP_*`。因此不要在 `api/aiQingdian.js` 里加 `typeof process !== "undefined"` 之类的运行时判断，否则会把已替换的地址短路成空串，请求退化成相对路径并 404。

## 页面能力

| 能力 | 实现位置 |
| --- | --- |
| 拍照 / 相册选图（压缩模式） | `index.vue` → `chooseImage()` |
| 调用 `/api/analyze` 并渲染汇总卡片 | `index.vue` → `analyze()` |
| 商品名搜索改写（修改行高亮） | `SearchSelect.vue` + `index.vue` → `selectProduct()` |
| 只提交被改动的行 | `correctionUtils.js` → `buildSavePayload()` |
| 双拼拆分 / 归一化提示 | `index.vue` 模板中的 `item-tips` 区块 |
| 识别失败时查看模型原始返回 | `index.vue` 末尾的 `raw-wrap` 区块 |

## 后端依赖

本模块依赖同仓库的 `backend/` 服务，接口约定见 [`../docs/api.md`](../docs/api.md)。
