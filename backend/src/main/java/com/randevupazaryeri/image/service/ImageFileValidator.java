package com.randevupazaryeri.image.service;

import com.randevupazaryeri.common.exception.ImageValidationException;
import com.randevupazaryeri.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

/**
 * Accepts only real JPEG / PNG / WebP (and optionally AVIF) images. The declared Content-Type must be
 * allowed and must match the file signature; the file extension and client file name are never trusted.
 * SVG, GIF, HEIC and anything else are rejected.
 */
@Component
@RequiredArgsConstructor
public class ImageFileValidator {

    static final String JPEG = "image/jpeg";
    static final String PNG = "image/png";
    static final String WEBP = "image/webp";
    static final String AVIF = "image/avif";

    private final UploadProperties properties;

    public record ValidatedImage(byte[] content, String contentType) {
    }

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ImageValidationException.invalid("Lütfen bir fotoğraf dosyası seçin.");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && filename.chars().anyMatch(c -> c == 0 || Character.isISOControl(c))) {
            throw ImageValidationException.invalid("Dosya adı geçersiz.");
        }
        long maxBytes = properties.getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw tooLarge();
        }

        String declared = normalize(file.getContentType());
        if (!isAllowed(declared)) {
            throw ImageValidationException.unsupportedType(supportedTypesMessage());
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw ImageValidationException.invalid("Dosya okunamadı.");
        }
        if (content.length == 0) {
            throw ImageValidationException.invalid("Lütfen bir fotoğraf dosyası seçin.");
        }
        if (content.length > maxBytes) {
            throw tooLarge();
        }

        String detected = detect(content);
        if (detected == null || !detected.equals(declared)) {
            throw ImageValidationException.invalid("Dosya geçerli bir fotoğraf değil.");
        }
        checkDimensions(detected, content);
        return new ValidatedImage(content, detected);
    }

    private boolean isAllowed(String contentType) {
        return JPEG.equals(contentType) || PNG.equals(contentType) || WEBP.equals(contentType)
                || (properties.isAllowAvif() && AVIF.equals(contentType));
    }

    private String supportedTypesMessage() {
        return properties.isAllowAvif()
                ? "Yalnızca JPEG, PNG, WebP veya AVIF fotoğraf yükleyebilirsiniz."
                : "Yalnızca JPEG, PNG veya WebP fotoğraf yükleyebilirsiniz.";
    }

    private ImageValidationException tooLarge() {
        return ImageValidationException.tooLarge(
                "Dosya boyutu en fazla " + properties.getMaxFileSize().toMegabytes() + " MB olabilir.");
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(type) || "image/pjpeg".equals(type) ? JPEG : type;
    }

    /** Identifies the format from its signature (magic bytes). */
    static String detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && ascii(b, 1, "PNG")
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        if (b.length >= 16 && ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")) {
            return WEBP;
        }
        if (b.length >= 16 && ascii(b, 4, "ftyp") && isAvifBrand(b)) {
            return AVIF;
        }
        return null;
    }

    private static boolean isAvifBrand(byte[] b) {
        int boxSize = readInt(b, 0);
        int end = Math.min(b.length, boxSize > 0 ? boxSize : 32);
        if (ascii(b, 8, "avif") || ascii(b, 8, "avis")) {
            return true;
        }
        for (int i = 16; i + 4 <= end; i += 4) {
            if (ascii(b, i, "avif") || ascii(b, i, "avis")) {
                return true;
            }
        }
        return false;
    }

    private void checkDimensions(String type, byte[] content) {
        int[] size = switch (type) {
            case JPEG, PNG -> imageIoSize(content);
            case WEBP -> webpSize(content);
            default -> null; // AVIF: decoded and normalized to WebP by the storage provider
        };
        if (size == null) {
            if (AVIF.equals(type)) {
                return;
            }
            throw ImageValidationException.invalid("Dosya geçerli bir fotoğraf değil.");
        }
        if (size[0] <= 0 || size[1] <= 0) {
            throw ImageValidationException.invalid("Dosya geçerli bir fotoğraf değil.");
        }
        if ((long) size[0] * size[1] > properties.getMaxPixels()) {
            throw ImageValidationException.invalid("Fotoğraf çözünürlüğü çok yüksek.");
        }
    }

    /** Reads only the header, so huge (bomb) images are never decoded. */
    private static int[] imageIoSize(byte[] content) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static int[] webpSize(byte[] b) {
        if (b.length < 30) {
            return null;
        }
        if (ascii(b, 12, "VP8X")) {
            return new int[]{1 + readUint24(b, 24), 1 + readUint24(b, 27)};
        }
        if (ascii(b, 12, "VP8 ")) {
            if ((b[23] & 0xFF) != 0x9D || (b[24] & 0xFF) != 0x01 || (b[25] & 0xFF) != 0x2A) {
                return null;
            }
            int width = ((b[26] & 0xFF) | (b[27] & 0xFF) << 8) & 0x3FFF;
            int height = ((b[28] & 0xFF) | (b[29] & 0xFF) << 8) & 0x3FFF;
            return new int[]{width, height};
        }
        if (ascii(b, 12, "VP8L")) {
            if ((b[20] & 0xFF) != 0x2F) {
                return null;
            }
            int b1 = b[21] & 0xFF, b2 = b[22] & 0xFF, b3 = b[23] & 0xFF, b4 = b[24] & 0xFF;
            int width = 1 + (((b2 & 0x3F) << 8) | b1);
            int height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >> 6));
            return new int[]{width, height};
        }
        return null;
    }

    private static boolean ascii(byte[] b, int offset, String text) {
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        if (offset + expected.length > b.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (b[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static int readInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) << 24 | (b[offset + 1] & 0xFF) << 16 | (b[offset + 2] & 0xFF) << 8 | (b[offset + 3] & 0xFF);
    }

    private static int readUint24(byte[] b, int offset) {
        return (b[offset] & 0xFF) | (b[offset + 1] & 0xFF) << 8 | (b[offset + 2] & 0xFF) << 16;
    }
}
