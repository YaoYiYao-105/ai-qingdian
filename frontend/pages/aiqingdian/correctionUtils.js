export function normalizeItems(items) {
  if (!Array.isArray(items)) return [];
  return items.map((item, index) => ({
    ...item,
    row: index + 1,
    origName: item.name || "",
    selectedName: item.name || "",
    model_raw_name: item.model_raw_name || item.name || "",
    unclear: false,
  }));
}

export function filterOptions(options, query) {
  const list = Array.isArray(options) ? options : [];
  const q = String(query || "").trim();
  if (!q) return list.slice();
  return list.filter((option) => String(option).indexOf(q) >= 0);
}

export function isModified(item) {
  return Boolean(
    item &&
      item.origName &&
      item.selectedName &&
      item.origName !== item.selectedName,
  );
}

function serializeItem(item, index) {
  return {
    row: item.row || index + 1,
    recognized_name: item.origName || "",
    selected_name: item.selectedName || "",
    model_raw_name: item.model_raw_name || "",
    fill_ratio: typeof item.fill_ratio === "number" ? item.fill_ratio : null,
    confidence: item.confidence || "",
    description: item.description || "",
    x: item.x == null ? null : item.x,
    y: item.y == null ? null : item.y,
    width: item.width == null ? null : item.width,
    height: item.height == null ? null : item.height,
  };
}

function buildCorrectionRows(items) {
  if (!Array.isArray(items)) return [];
  const rows = [];
  items.forEach((item, index) => {
    const changed = isModified(item);
    const unclear = Boolean(item.unclear);
    if (!changed && !unclear) return;

    rows.push({
      row: item.row || index + 1,
      recognized_name: item.origName || "",
      corrected_name: !unclear && changed ? item.selectedName : "",
      model_raw_name: item.model_raw_name || "",
      unclear,
      fill_ratio: typeof item.fill_ratio === "number" ? item.fill_ratio : null,
      confidence: item.confidence || "",
      description: item.description || "",
    });
  });
  return rows;
}

export function buildSavePayload(data) {
  const items = Array.isArray(data.items) ? data.items : [];
  return {
    raw_model_text: data.raw || "",
    items: items.map(serializeItem),
    corrections: buildCorrectionRows(items),
  };
}
