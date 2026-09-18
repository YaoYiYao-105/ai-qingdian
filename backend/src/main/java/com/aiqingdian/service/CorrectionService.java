package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.config.CorrectionProperties;
import com.aiqingdian.dto.CorrectionRecord;
import com.aiqingdian.dto.CorrectionRequest;
import com.aiqingdian.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 保存人工纠错训练数据：
 * 原图落盘 + 完整识别上下文与修正列表追加写入 corrections.jsonl。
 */
@Service
public class CorrectionService {

    private static final Logger log = LoggerFactory.getLogger(CorrectionService.class);

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/bmp", "image/gif"
    );

    private final CorrectionProperties properties;
    private final ArkProperties arkProperties;
    private final ProductCatalogService catalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    public CorrectionService(CorrectionProperties properties,
                             ArkProperties arkProperties,
                             ProductCatalogService catalogService) {
        this.properties = properties;
        this.arkProperties = arkProperties;
        this.catalogService = catalogService;
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 保存一次纠错会话。返回 {session_id, image_file, corrections_count}。
     */
    public Map<String, Object> save(MultipartFile file, String json) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请上传图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("不支持的图片格式，请上传 jpg/png/bmp/gif 图片");
        }

        CorrectionRequest request = parse(json);
        validate(request);

        byte[] imageBytes = file.getBytes();
        String sha256 = sha256(imageBytes);
        Path root = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();

        lock.lock();
        try {
            String imageFile = findExistingImageFile(root, sha256);
            if (imageFile == null) {
                imageFile = writeImage(root, imageBytes, sha256, file.getOriginalFilename());
            }

            CorrectionRecord record = new CorrectionRecord();
            record.setSessionId(UUID.randomUUID().toString().replace("-", ""));
            record.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            CorrectionRecord.CorrectionImage image = new CorrectionRecord.CorrectionImage();
            image.setFile(imageFile);
            image.setSha256(sha256);
            record.setImage(image);

            record.setModel(arkProperties.getModel());
            record.setRawModelText(request.getRawModelText());
            record.setItems(request.getItems());
            record.setCorrections(request.getCorrections());

            appendLine(root.resolve("corrections.jsonl"), objectMapper.writeValueAsString(record));

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("session_id", record.getSessionId());
            result.put("image_file", imageFile);
            result.put("corrections_count", request.getCorrections().size());
            log.info("保存纠错样本成功：session_id={}，修正 {} 条，图片 {}", record.getSessionId(),
                    request.getCorrections().size(), imageFile);
            return result;
        } finally {
            lock.unlock();
        }
    }

    private CorrectionRequest parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new BadRequestException("修正数据不能为空");
        }
        try {
            return objectMapper.readValue(json, CorrectionRequest.class);
        } catch (IOException e) {
            throw new BadRequestException("修正数据 JSON 无法解析：" + abbreviate(e.getMessage()));
        }
    }

    private void validate(CorrectionRequest request) {
        if (request.getCorrections() == null || request.getCorrections().isEmpty()) {
            throw new BadRequestException("没有需要保存的修正");
        }
        List<CorrectionRequest.CorrectionItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("识别结果列表不能为空");
        }
        for (CorrectionRequest.CorrectionEntry entry : request.getCorrections()) {
            int row = entry.getRow();
            if (row < 1 || row > items.size()) {
                throw new BadRequestException("修正行号超出识别结果范围：" + row);
            }
            if (!entry.isUnclear()) {
                String correctedName = entry.getCorrectedName();
                if (correctedName == null || correctedName.trim().isEmpty()) {
                    throw new BadRequestException("第 " + row + " 行修改后的商品名不能为空");
                }
                if (!catalogService.isOfficialName(correctedName)) {
                    throw new BadRequestException("修正后的商品不在门店清单中：" + correctedName);
                }
            } else {
                // 看不清的行不保留修正名，避免被误当成训练正样本
                entry.setCorrectedName(null);
            }
        }
    }

    private String findExistingImageFile(Path root, String sha256) throws IOException {
        Path metadata = root.resolve("corrections.jsonl");
        if (!Files.exists(metadata)) {
            return null;
        }
        List<String> lines = Files.readAllLines(metadata, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            JsonNode node = objectMapper.readTree(trimmed);
            JsonNode imageNode = node.path("image");
            if (sha256.equals(imageNode.path("sha256").asText(""))) {
                String file = imageNode.path("file").asText("");
                if (!file.isEmpty()) {
                    return file;
                }
            }
        }
        return null;
    }

    private String writeImage(Path root, byte[] bytes, String sha256, String originalName) throws IOException {
        String ext = detectExtension(bytes, originalName);
        LocalDate now = LocalDate.now();
        Path dir = root.resolve("images")
                .resolve(String.valueOf(now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()))
                .resolve(String.format("%02d", now.getDayOfMonth()));
        Files.createDirectories(dir);

        Path target = dir.resolve(sha256 + "." + ext);
        if (!Files.exists(target)) {
            Files.write(target, bytes);
        }
        return root.relativize(target).toString();
    }

    private void appendLine(Path file, String line) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file,
                (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private String detectExtension(byte[] bytes, String originalName) {
        if (bytes != null && bytes.length >= 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            if (b0 == 0xff && b1 == 0xd8) {
                return "jpg";
            }
            if (b0 == 0x89 && b1 == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) {
                return "png";
            }
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
                return "gif";
            }
            if (b0 == 0x42 && b1 == 0x4d) {
                return "bmp";
            }
        }
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                String ext = originalName.substring(dot + 1).toLowerCase();
                if (SUPPORTED_TYPES.contains("image/" + ext) || "jpg".equals(ext) || "jpeg".equals(ext)) {
                    return "jpeg".equals(ext) ? "jpg" : ext;
                }
            }
        }
        return "jpg";
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200) + "...";
    }
}
