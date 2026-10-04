package com.randevupazaryeri.image.storage;

/**
 * An already validated image to store.
 *
 * @param directory server-built folder path (never user input), e.g. {@code resplz/businesses/{id}/cover}
 * @param name      server-generated object name (never the client file name)
 * @param maxWidth  images wider than this are scaled down; never upscaled
 */
public record ImageUpload(byte[] content, String contentType, String directory, String name, int maxWidth) {

    @Override
    public String toString() {
        return "ImageUpload{directory=" + directory + ", name=" + name + ", contentType=" + contentType
                + ", bytes=" + (content == null ? 0 : content.length) + ", maxWidth=" + maxWidth + "}";
    }
}
