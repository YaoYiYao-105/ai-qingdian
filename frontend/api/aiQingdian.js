/**
 * AI 熟食展示柜识别接口（ai-qingdian 模块）
 *
 * 后端：DigitalStoreApplication 中的 AnalyzeController
 *   GET  /api/products      商品清单
 *   POST /api/analyze       上传图片识别剩余量
 *   POST /api/corrections   保存人工纠错训练样本
 *
 * 后端统一返回：{ code, message, data }，code === 0 表示成功。
 */

function normalizeBaseUrl(url) {
  return String(url || "").replace(/\/+$/, "");
}

function readAiQingdianBaseUrl() {
  // 与 utils/request/request.js 读取 VUE_APP_BaseUrl 的方式保持一致：
  // HBuilderX 发行时，构建工具会把 process.env.VUE_APP_AIQingdianBaseUrl 静态替换成真实地址。
  // 注意：不要加 typeof process !== "undefined" 之类的运行时判断，否则会把已被静态替换的地址短路掉，
  // 导致 baseUrl 为空，请求变成相对路径（例如 https://zs.cqmzz.com/api/products 404）。
  const baseUrl = process.env.VUE_APP_AIQingdianBaseUrl;
  if (baseUrl) return normalizeBaseUrl(baseUrl);
  return "";
}

export const AI_QINGDIAN_BASE_URL = readAiQingdianBaseUrl();

function requestJson(path, method = "GET", data = {}) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${AI_QINGDIAN_BASE_URL}${path}`,
      method,
      data,
      timeout: 100000,
      success(response) {
        const responseData = response.data || {};
        if (responseData.code === 0) {
          resolve(responseData);
          return;
        }

        const message =
          responseData.message ||
          responseData.msg ||
          "服务响应失败";
        uni.showToast({ icon: "none", duration: 4000, title: message });
        reject(responseData);
      },
      fail(error) {
        uni.showToast({ icon: "none", title: "服务响应失败" });
        reject(error);
      },
    });
  });
}

function uploadJson(path, filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${AI_QINGDIAN_BASE_URL}${path}`,
      filePath,
      name: "file",
      formData,
      timeout: 180000,
      success(response) {
        let responseData = {};
        try {
          responseData = JSON.parse(response.data || "{}");
        } catch (error) {
          reject(new Error("服务器返回内容无法解析"));
          return;
        }

        if (responseData.code === 0) {
          resolve(responseData);
          return;
        }

        const message =
          responseData.message ||
          responseData.msg ||
          "服务响应失败";
        uni.showToast({ icon: "none", duration: 4000, title: message });
        reject(responseData);
      },
      fail(error) {
        uni.showToast({ icon: "none", title: "上传失败" });
        reject(error);
      },
    });
  });
}

/** 获取商品清单 */
export const getProductCatalogApi = () => requestJson("/api/products");

/** 上传展示柜照片并识别剩余量 */
export const analyzeImageApi = (filePath) => uploadJson("/api/analyze", filePath);

/** 保存人工纠错训练样本 */
export const saveCorrectionsApi = (filePath, payload) =>
  uploadJson("/api/corrections", filePath, {
    json: JSON.stringify(payload),
  });
