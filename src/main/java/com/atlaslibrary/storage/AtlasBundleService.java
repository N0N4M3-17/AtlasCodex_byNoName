package com.atlaslibrary.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class AtlasBundleService {
    private final ObjectMapper mapper = new ObjectMapper();

    public void writeManifestAndDb(Path bundlePath, Path sqlitePath) throws IOException {
        Files.createDirectories(bundlePath.getParent());
        try (OutputStream out = Files.newOutputStream(bundlePath);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(mapper.writeValueAsBytes(Map.of("format", "atlaslib", "version", "v0.0.1ab")));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("db/project.sqlite"));
            Files.copy(sqlitePath, zip);
            zip.closeEntry();
        }
    }

    public Path extractDb(Path bundlePath, Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("db"));
        Path db = tempDir.resolve("db/project.sqlite");
        try (ZipFile zip = new ZipFile(bundlePath.toFile())) {
            ZipEntry entry = zip.getEntry("db/project.sqlite");
            if (entry == null) throw new IOException("db/project.sqlite missing");
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, db, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return db;
    }
}
