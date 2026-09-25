package com.marlus.financas.dataio.service;

import java.util.List;

public record ExportManifest(String schemaVersion, String exportedAt, String appVersion, List<ManifestFile> files) {

    public record ManifestFile(String name, int rows, String sha256) {
    }
}
