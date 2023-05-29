package com.grokonez.excelfile.fileservice;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CsvStorageService {

	public void init();

	public Resource load(String filename);

	public void deleteAll();

	public Stream<Path> loadAll();

	public void saveAndGroupCdr(MultipartFile file);
}
