package com.grokonez.excelfile.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.grokonez.excelfile.fileservice.ShopeeStorageService;

@RestController
@RequestMapping("/upload")
public class PostManUploadFileController {
	
	@Autowired
	ShopeeStorageService shopeeService;

	@PostMapping(path="/shopee")
	public String uploadGroupShopee(@RequestParam("shopee") MultipartFile file, Model model) {
		String message = "";
		try {
			List<String> fileNames = new ArrayList<>();
			shopeeService.saveAndGroupShopee(file);
	        fileNames.add(file.getOriginalFilename());
	        message = "Uploaded the Shopee files successfully: " + fileNames;
			model.addAttribute("messageShopee", message);
			model.addAttribute("pathShopee", "Path Grouping ==> WORKSPECE Project UploadGroupSerder folder uploadsShopee");
			
		} catch (Exception e) {
			model.addAttribute("messageShopee", "Fail! -> uploaded filename: " + file.getOriginalFilename());
		}
		return message;
	}
}