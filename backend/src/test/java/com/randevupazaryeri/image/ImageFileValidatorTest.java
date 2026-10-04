package com.randevupazaryeri.image;

import com.randevupazaryeri.common.exception.ErrorCode;
import com.randevupazaryeri.common.exception.ImageValidationException;
import com.randevupazaryeri.config.UploadProperties;
import com.randevupazaryeri.image.service.ImageFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {

    UploadProperties properties;
    ImageFileValidator validator;

    @BeforeEach
    void setUp() {
        properties = new UploadProperties();
        validator = new ImageFileValidator(properties);
    }

    @Test
    void acceptsRealJpegPngAndWebp() {
        assertThat(validator.validate(file("a.jpg", "image/jpeg", TestImages.jpeg(40, 30))).contentType())
                .isEqualTo("image/jpeg");
        assertThat(validator.validate(file("a.png", "image/png", TestImages.png(40, 30))).contentType())
                .isEqualTo("image/png");
        assertThat(validator.validate(file("a.webp", "image/webp", TestImages.WEBP)).contentType())
                .isEqualTo("image/webp");
    }

    @Test
    void acceptsCommonJpegAliasAndContentTypeParameters() {
        assertThat(validator.validate(file("a.jpg", "image/jpg", TestImages.jpeg(10, 10))).contentType())
                .isEqualTo("image/jpeg");
        assertThat(validator.validate(file("a.png", "IMAGE/PNG; charset=binary", TestImages.png(10, 10))).contentType())
                .isEqualTo("image/png");
    }

    @Test
    void rejectsSvgAndOtherTypesAsUnsupported() {
        assertRejected(file("x.svg", "image/svg+xml", TestImages.SVG), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertRejected(file("x.gif", "image/gif", "GIF89a....".getBytes(StandardCharsets.US_ASCII)),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertRejected(file("x.png", "application/octet-stream", TestImages.png(5, 5)), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertRejected(file("x.png", null, TestImages.png(5, 5)), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsSpoofedContentWhoseSignatureDoesNotMatch() {
        assertRejected(file("evil.jpg", "image/jpeg", TestImages.SVG), HttpStatus.BAD_REQUEST);
        assertRejected(file("evil.png", "image/png", "MZ\u0090\u0000 not an image".getBytes(StandardCharsets.ISO_8859_1)),
                HttpStatus.BAD_REQUEST);
        assertRejected(file("photo.jpg", "image/jpeg", TestImages.png(5, 5)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTruncatedImageWithValidSignature() {
        byte[] png = TestImages.png(5, 5);
        byte[] truncated = java.util.Arrays.copyOf(png, 12);
        assertRejected(file("broken.png", "image/png", truncated), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsOversizedFile() {
        properties.setMaxFileSize(DataSize.ofKilobytes(1));
        assertRejected(file("big.png", "image/png", new byte[2048]), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void rejectsEmptyFileAndControlCharactersInName() {
        assertRejected(file("a.png", "image/png", new byte[0]), HttpStatus.BAD_REQUEST);
        assertRejected(file("a\u0000.png", "image/png", TestImages.png(5, 5)), HttpStatus.BAD_REQUEST);
        assertRejected(null, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsImagesAboveThePixelLimit() {
        properties.setMaxPixels(100);
        assertRejected(file("huge.png", "image/png", TestImages.png(20, 20)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void avifCanBeDisabled() {
        byte[] avif = new byte[32];
        System.arraycopy(new byte[]{0, 0, 0, 0x20}, 0, avif, 0, 4);
        System.arraycopy("ftypavif".getBytes(StandardCharsets.US_ASCII), 0, avif, 4, 8);

        assertThat(validator.validate(file("a.avif", "image/avif", avif)).contentType()).isEqualTo("image/avif");

        properties.setAllowAvif(false);
        assertRejected(file("a.avif", "image/avif", avif), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    private void assertRejected(MockMultipartFile file, HttpStatus status) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ImageValidationException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    if (status == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                        assertThat(e.getCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
                    }
                });
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}
