package com.grokonez.excelfile.fileservice;

import org.springframework.web.multipart.MultipartFile;

public interface AntsStorageService {

	public void init();

	public void saveAndFindDuplicate(MultipartFile file);

}
