<template>
  <view class="ai-qingdian-page">
    <view class="page-header">
      <view class="page-header__title">AI 商品识别</view>
      <view class="page-header__desc">
        上传熟食展示柜照片，自动判断每个餐盘剩余量是否充足
      </view>
    </view>

    <view
      class="upload-card"
      :class="{ dragging }"
      @tap="chooseImage"
    >
      <view class="upload-card__icon">📷</view>
      <view class="upload-card__tip">
        点击选择图片，或将图片拖拽到此处
        <text class="upload-card__sub">支持 jpg / png / bmp / gif</text>
      </view>
    </view>

    <view v-if="previewUrl" class="preview-card">
      <image class="preview-card__image" :src="previewUrl" mode="widthFix" />
      <view class="preview-card__actions">
        <button
          class="action-btn action-btn--primary"
          :disabled="loading"
          @tap="analyze"
        >
          {{ loading ? "识别中…" : "开始识别" }}
        </button>
        <button
          class="action-btn action-btn--plain"
          :disabled="loading"
          @tap="reset"
        >
          重新选择
        </button>
      </view>
    </view>

    <view v-if="loading" class="loading">
      <view class="loading__spinner"></view>
      <text>正在识别，请稍候（约 3-10 秒）…</text>
    </view>

    <view v-if="error" class="error-card">{{ error }}</view>

    <view v-if="catalogError" class="error-card error-card--catalog">
      商品清单加载失败：{{ catalogError }}
      <text class="error-card__retry" @tap="loadCatalog">点击重试</text>
    </view>

    <view v-if="result && result.code === 0" class="result-card">
      <view class="result-card__summary">
        <view class="summary-item">
          <view
            class="summary-item__num"
            :class="levelClass(result.data.stock_level)"
          >
            {{ result.data.stock_level || "未知" }}
          </view>
          <view class="summary-item__label">整体库存水平</view>
        </view>
        <view class="summary-item">
          <view class="summary-item__num summary-item__num--dark">
            {{ result.data.stock_score >= 0 ? result.data.stock_score + "%" : "—" }}
          </view>
          <view class="summary-item__label">平均剩余占比</view>
        </view>
        <view class="summary-item">
          <view
            class="summary-item__num summary-item__num--small"
            :class="levelClass(result.data.stock_level)"
          >
            {{ result.data.order_reasonableness || "—" }}
          </view>
          <view class="summary-item__label">订货合理性</view>
        </view>
      </view>

      <view
        v-if="
          result.data.restock_suggestions &&
          result.data.restock_suggestions.length
        "
        class="suggest-card"
      >
        <view class="suggest-card__title">
          ⚠️ 建议补货（{{ result.data.restock_suggestions.length }} 盘剩余偏少）
        </view>
        <view
          v-for="(item, index) in result.data.restock_suggestions"
          :key="'suggest-' + index"
          class="suggest-card__item"
        >
          <text>{{ item.name }}</text>
          <text class="badge" :class="levelClass(item.level)">
            {{ item.level }}（{{ item.fill_ratio }}%）
          </text>
        </view>
      </view>

      <view
        v-if="result.data.items && result.data.items.length"
        class="items-list"
      >
        <view class="items-list__head">
          <text class="items-list__cell items-list__cell--name">商品名</text>
          <text class="items-list__cell items-list__cell--ratio">剩余量占比</text>
          <text class="items-list__cell items-list__cell--level">库存水平</text>
          <text class="items-list__cell items-list__cell--conf">置信度</text>
        </view>
        <view
          v-for="(item, index) in result.data.items"
          :key="index"
          class="item-row"
        >
          <view class="items-list__cell items-list__cell--name">
            <SearchSelect
              :model-value="item.selectedName"
              :options="catalog"
              :edited="isModified(item)"
              @update:model-value="selectProduct(item, $event)"
              @focus="onCatalogFocus"
            />
          </view>

          <view class="items-list__cell items-list__cell--ratio">
            <view class="ratio-wrap">
              <view class="ratio-bar" :style="ratioBarStyle(item.fill_ratio)"></view>
              <text class="ratio-text">
                {{ item.fill_ratio >= 0 ? item.fill_ratio + "%" : "—" }}
              </text>
            </view>
          </view>

          <view class="items-list__cell items-list__cell--level">
            <text class="badge" :class="levelClass(item.level)">
              {{ item.level || "未知" }}
            </text>
          </view>

          <view class="items-list__cell items-list__cell--conf">
            <text class="badge" :class="confidenceClass(item.confidence)">
              {{ confidenceText(item.confidence) }}
            </text>
          </view>
        </view>
        <template v-for="(item, index) in result.data.items" :key="'tips-' + index">
          <view
            v-if="
              isModified(item) ||
              item.source === 'split' ||
              (item.original_name && item.original_name !== item.name)
            "
            class="item-tips"
          >
            <view class="item-tips__inner">
              <view v-if="isModified(item)" class="modified-tip">
                原识别：{{ item.origName }}
              </view>
              <view v-if="item.source === 'split'" class="origin-tip">
                已按双拼拆分
              </view>
              <view
                v-if="item.original_name && item.original_name !== item.name"
                class="origin-tip"
              >
                模型：{{ item.original_name }}
              </view>
            </view>
          </view>
        </template>
      </view>
      <view v-else class="empty-result">未识别到展示餐盘。</view>

      <button
        v-if="result.data.items && result.data.items.length"
        class="save-btn"
        :disabled="saving || !hasEdits"
        @tap="saveCorrections"
      >
        {{ saving ? "保存中…" : "确定" }}
      </button>
    </view>

    <view
      v-if="result && result.code !== 0 && result.data && result.data.raw"
      class="result-card"
    >
      <view class="error-card error-card--inline">{{ result.message }}</view>
      <view class="raw-wrap">
        <view class="raw-wrap__title">查看模型原始返回（排查用）</view>
        <text class="raw-wrap__content">{{ result.data.raw }}</text>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import SearchSelect from "./SearchSelect.vue";
import {
  getProductCatalogApi,
  analyzeImageApi,
  saveCorrectionsApi,
} from "@/api/aiQingdian.js";
import {
  normalizeItems,
  isModified as checkModified,
  buildSavePayload,
} from "./correctionUtils.js";

const previewUrl = ref("");
const uploadFile = ref("");
const loading = ref(false);
const saving = ref(false);
const result = ref(null);
const error = ref("");
const dragging = ref(false);
const catalog = ref([]);
const catalogLoading = ref(false);
const catalogError = ref("");

const hasEdits = computed(() => {
  const items = result.value && result.value.data && result.value.data.items;
  if (!Array.isArray(items)) return false;
  return items.some((item) => checkModified(item));
});

async function loadCatalog() {
  if (catalogLoading.value) return;
  catalogLoading.value = true;
  catalogError.value = "";
  try {
    const body = await getProductCatalogApi();
    catalog.value = (body.data || []).map((item) => item.name);
  } catch (e) {
    catalogError.value = (e && e.message) || "网络错误";
  } finally {
    catalogLoading.value = false;
  }
}

function onCatalogFocus() {
  if (!catalog.value.length && !catalogLoading.value) {
    loadCatalog();
  }
}

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ["compressed"],
    sourceType: ["album", "camera"],
    success(res) {
      const filePath = res.tempFilePaths && res.tempFilePaths[0];
      if (!filePath) return;
      error.value = "";
      result.value = null;
      uploadFile.value = filePath;
      previewUrl.value = filePath;
    },
    fail(err) {
      if (String(err && err.errMsg || "").indexOf("cancel") === -1) {
        uni.showToast({ icon: "none", title: "选择图片失败" });
      }
    },
  });
}

function isModified(item) {
  return checkModified(item);
}

function selectProduct(item, value) {
  item.selectedName = value;
}

async function analyze() {
  if (!uploadFile.value) {
    error.value = "请先选择图片";
    return;
  }

  loading.value = true;
  error.value = "";
  result.value = null;
  try {
    const body = await analyzeImageApi(uploadFile.value);
    result.value = body;
    if (body.code === 0 && body.data && Array.isArray(body.data.items)) {
      body.data.items = normalizeItems(body.data.items);
    }
    if (body.code !== 0) {
      error.value = body.message || "识别失败";
    }
  } catch (e) {
    error.value = "请求失败：" + ((e && e.message) || e);
  } finally {
    loading.value = false;
  }
}

async function saveCorrections() {
  if (!uploadFile.value || !result.value || !result.value.data) return;
  const payload = buildSavePayload(result.value.data);
  if (!payload.corrections.length) {
    uni.showToast({ icon: "none", title: "没有修改过的行，无需保存" });
    return;
  }

  saving.value = true;
  try {
    const body = await saveCorrectionsApi(uploadFile.value, payload);
    uni.showToast({
      icon: "success",
      title: body.code === 0 ? "样本已保存" : body.message || "保存失败",
    });
  } catch (e) {
    uni.showToast({
      icon: "none",
      title: "保存失败：" + ((e && e.message) || e),
    });
  } finally {
    saving.value = false;
  }
}

function reset() {
  previewUrl.value = "";
  uploadFile.value = "";
  result.value = null;
  error.value = "";
  saving.value = false;
}

function confidenceClass(confidence) {
  const value = String(confidence || "").toLowerCase();
  if (value === "high") return "badge-high";
  if (value === "medium") return "badge-medium";
  if (value === "low") return "badge-low";
  return "";
}

function confidenceText(confidence) {
  const value = String(confidence || "").toLowerCase();
  if (value === "high") return "高";
  if (value === "medium") return "中";
  if (value === "low") return "低";
  return confidence || "未知";
}

function levelClass(level) {
  if (level === "充足") return "badge-high";
  if (level === "一般") return "badge-medium";
  if (level === "较少") return "badge-low";
  if (level === "严重不足") return "badge-critical";
  return "";
}

function ratioBarStyle(ratio) {
  const value = typeof ratio === "number" && ratio >= 0 ? Math.min(100, ratio) : 0;
  let color = "#1e90ff";
  if (value < 15) color = "#e74c3c";
  else if (value < 40) color = "#e67e22";
  else if (value < 70) color = "#f1c40f";
  return { width: value + "%", background: color };
}

onMounted(() => {
  loadCatalog();
});
</script>

<style lang="scss" scoped>
.ai-qingdian-page {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 32rpx 24rpx 80rpx;
  box-sizing: border-box;
  background: #f5f6fa;
  color: #2f3542;
}

.page-header {
  margin-bottom: 32rpx;
  text-align: center;

  &__title {
    font-size: 44rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #747d8c;
  }
}

.upload-card {
  padding: 64rpx 24rpx;
  text-align: center;
  background: #fff;
  border: 2rpx dashed #b2bec3;
  border-radius: 24rpx;

  &.dragging {
    border-color: #1e90ff;
    background: #f0f8ff;
  }

  &__icon {
    font-size: 72rpx;
  }

  &__tip {
    margin-top: 16rpx;
    font-size: 28rpx;
    color: #747d8c;
  }

  &__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    color: #a4b0be;
  }
}

.preview-card {
  margin-top: 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);

  &__image {
    width: 100%;
    border-radius: 16rpx;
    background: #f1f2f6;
  }

  &__actions {
    display: flex;
    gap: 20rpx;
    margin-top: 24rpx;
  }
}

.action-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border: none;
  border-radius: 16rpx;
  font-size: 30rpx;

  &::after {
    border: none;
  }

  &--primary {
    color: #fff;
    background: #1e90ff;

    &[disabled] {
      color: #fff;
      background: #a9c9ea;
    }
  }

  &--plain {
    color: #2f3542;
    background: #dfe4ea;
  }
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 32rpx;
  font-size: 28rpx;
  color: #1e90ff;

  &__spinner {
    width: 32rpx;
    height: 32rpx;
    margin-right: 16rpx;
    border: 6rpx solid #d1e7ff;
    border-top-color: #1e90ff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

.error-card {
  margin-top: 24rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #c0392b;
  background: #fdecea;
  border-radius: 16rpx;

  &--catalog {
    margin-top: 20rpx;
  }

  &--inline {
    margin-top: 0;
  }

  &__retry {
    margin-left: 12rpx;
    color: #1e90ff;
  }
}

.result-card {
  margin-top: 24rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);

  &__summary {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16rpx;
    padding-bottom: 24rpx;
    margin-bottom: 24rpx;
    border-bottom: 1rpx solid #f1f2f6;
  }
}

.summary-item {
  min-width: 0;
  text-align: center;

  &__num {
    font-size: 40rpx;
    font-weight: 700;
    color: #1e90ff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &--dark {
      color: #2f3542;
      font-size: 34rpx;
    }

    &--small {
      font-size: 26rpx;
    }
  }

  &__label {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #747d8c;
    white-space: nowrap;
  }
}

.suggest-card {
  margin-bottom: 24rpx;
  padding: 22rpx 26rpx;
  background: #fff8e1;
  border: 1rpx solid #ffe082;
  border-radius: 16rpx;

  &__title {
    margin-bottom: 12rpx;
    font-weight: 600;
    color: #856404;
  }

  &__item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8rpx 0;
    font-size: 26rpx;
  }
}

.items-list {
  padding-top: 4rpx;
}

.items-list__head,
.item-row {
  display: flex;
  align-items: center;
}

.items-list__head {
  height: 56rpx;
  color: #747d8c;
  font-size: 22rpx;
  border-bottom: 1rpx solid #f1f2f6;
}

.item-row {
  min-height: 88rpx;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f1f2f6;
}

.items-list__cell {
  min-width: 0;
  font-size: 22rpx;

  &--name {
    width: 50%;
  }

  &--ratio {
    width: 26%;
    padding: 0 6rpx;
  }

  &--level,
  &--conf {
    width: 18%;
    text-align: center;
  }
}

.item-tips {
  padding: 4rpx 0 10rpx;
  border-bottom: 1rpx solid #f1f2f6;

  &__inner {
    padding-left: 2rpx;
  }
}

.badge {
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 18rpx;
  font-size: 22rpx;
}

.badge-high {
  color: #155724;
  background: #d4edda;
}

.badge-medium {
  color: #856404;
  background: #fff3cd;
}

.badge-low {
  color: #721c24;
  background: #f8d7da;
}

.badge-critical {
  color: #fff;
  background: #dc3545;
}

.ratio-wrap {
  position: relative;
  height: 36rpx;
  background: #f1f2f6;
  border-radius: 8rpx;
  overflow: hidden;
}

.ratio-bar {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  border-radius: 8rpx;
}

.ratio-text {
  position: relative;
  display: block;
  text-align: center;
  line-height: 36rpx;
  font-size: 22rpx;
  color: #2f3542;
}

.modified-tip {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #1e90ff;
}

.origin-tip {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #a4b0be;
}

.save-btn {
  margin-top: 24rpx;
  color: #fff;
  background: #2ed573;
  border-radius: 16rpx;

  &::after {
    border: none;
  }

  &[disabled] {
    color: #fff;
    background: #a8d5ba;
  }
}

.empty-result {
  padding: 24rpx;
  font-size: 26rpx;
  color: #747d8c;
  background: #f8f9fa;
  border-radius: 16rpx;
}

.raw-wrap {
  margin-top: 24rpx;

  &__title {
    font-size: 26rpx;
    color: #747d8c;
  }

  &__content {
    display: block;
    max-height: 480rpx;
    margin-top: 12rpx;
    padding: 20rpx;
    overflow: auto;
    font-size: 22rpx;
    line-height: 1.6;
    color: #2f3542;
    background: #f1f2f6;
    border-radius: 16rpx;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
