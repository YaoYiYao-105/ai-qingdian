<template>
  <view class="search-select">
    <input
      class="search-select__input"
      :class="{ 'is-modified': edited }"
      :value="query === null ? modelValue : query"
      placeholder="搜索/选择商品"
      @focus="onFocus"
      @blur="onBlur"
      @input="onInput"
    />
    <view v-if="open" class="search-select__panel">
      <scroll-view scroll-y class="search-select__scroll">
        <view
          v-for="option in filtered"
          :key="option"
          class="search-select__item"
          :class="{ selected: option === modelValue }"
          @tap.stop="select(option)"
        >
          {{ option }}
        </view>
        <view v-if="!filtered.length" class="search-select__empty">
          无匹配商品
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from "vue";
import { filterOptions } from "./correctionUtils.js";

const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
  options: {
    type: Array,
    default: () => [],
  },
  edited: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue", "focus"]);

const query = ref(null);
const open = ref(false);
let blurTimer = null;

const filtered = computed(() => filterOptions(props.options, query.value));

function onFocus() {
  open.value = true;
  emit("focus");
}

function onBlur() {
  blurTimer = setTimeout(() => {
    open.value = false;
    query.value = null;
  }, 180);
}

function onInput(event) {
  query.value = event.detail.value;
  open.value = true;
}

function select(option) {
  if (blurTimer) clearTimeout(blurTimer);
  emit("update:modelValue", option);
  open.value = false;
  query.value = null;
}
</script>

<style lang="scss" scoped>
.search-select {
  position: relative;
  min-width: 0;
  max-width: 240rpx;

  &__input {
    width: 100%;
    height: 64rpx;
    padding: 0 14rpx;
    font-size: 26rpx;
    color: #2f3542;
    border: 1rpx solid #dfe4ea;
    border-radius: 10rpx;
    background: #fff;

    &.is-modified {
      border-color: #1e90ff;
      background: #f0f8ff;
    }
  }

  &__panel {
    position: absolute;
    z-index: 20;
    top: 58rpx;
    left: 0;
    right: 0;
    background: #fff;
    border: 1rpx solid #dfe4ea;
    border-radius: 12rpx;
    box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
  }

  &__scroll {
    max-height: 400rpx;
  }

  &__item {
    height: 56rpx;
    line-height: 56rpx;
    padding: 0 16rpx;
    font-size: 22rpx;
    color: #2f3542;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &.selected {
      color: #1e90ff;
      background: #eaf4ff;
    }
  }

  &__empty {
    padding: 16rpx;
    font-size: 22rpx;
    color: #a4b0be;
    text-align: center;
  }
}
</style>
