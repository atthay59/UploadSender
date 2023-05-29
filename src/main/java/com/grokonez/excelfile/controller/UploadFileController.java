package com.grokonez.excelfile.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.grokonez.excelfile.fileservice.CsvStorageService;
import com.grokonez.excelfile.fileservice.FilesStorageService;

@Controller
public class UploadFileController {

	@Autowired
	FilesStorageService storageService;
	
	@Autowired
	CsvStorageService csvService;
	
	@GetMapping("/")
	public String index() {
		return "multipartfile/uploadform.html";
	}
	
	@GetMapping("/cdr-log")
	public String cdrForm() {
		return "cdr/cdrform.html";
	}

	@PostMapping("/")
	public String uploadMultipartFile(@RequestParam("uploadfile") MultipartFile file, Model model) {
		String message = "";
		try {
			List<String> fileNames = new ArrayList<>();
			//storageService.save(file);
			storageService.saveAndGroupAmsisdnraw(file);
	        fileNames.add(file.getOriginalFilename());
	        message = "Uploaded the files successfully: " + fileNames;
			model.addAttribute("message", message);
			model.addAttribute("path", "Path Grouping ==> D:\\@True\\GitlabRepository\\UploadGroupSerder\\uploads");
			
		} catch (Exception e) {
			model.addAttribute("message", "Fail! -> uploaded filename: " + file.getOriginalFilename());
		}
		return "multipartfile/uploadform.html";
	}
	
	@PostMapping("/uploadSpam")
	public String uploadSpamFile(@RequestParam("uploadSpam") MultipartFile file, Model model) {
		String message = "";
		try {
			List<String> fileNames = new ArrayList<>();
			//storageService.save(file);
			storageService.saveAndGroupSpam(file);
	        fileNames.add(file.getOriginalFilename());
	        message = "Uploaded the files successfully: " + fileNames;
			model.addAttribute("messageSpam", message);
			model.addAttribute("pathSpam", "Path Grouping ==> D:\\@True\\GitlabRepository\\UploadGroupSerder\\uploadsSpam");
			
		} catch (Exception e) {
			model.addAttribute("messageSpam", "Fail! -> uploaded filename: " + file.getOriginalFilename());
		}
		return "multipartfile/uploadform.html";
	}
	
	@PostMapping("/uploadCdr")
	public String uploadCdrFile(@RequestParam("uploadcdr") MultipartFile file, Model model) {
		String message = "";
		try {
			List<String> fileNames = new ArrayList<>();
			csvService.saveAndGroupCdr(file);
	        fileNames.add(file.getOriginalFilename());
	        message = "Uploaded the CDR files successfully: " + fileNames;
			model.addAttribute("messageCdr", message);
			model.addAttribute("pathCdr", "Path Grouping ==> WORKSPECE Project UploadGroupSerder folder uploadsCdr");
			
		} catch (Exception e) {
			model.addAttribute("messageCdr", "Fail! -> uploaded filename: " + file.getOriginalFilename());
		}
		return "cdr/cdrform.html";
	}
	
}