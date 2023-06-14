package com.grokonez.excelfile.fileservice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AntsStorageServiceImpl implements AntsStorageService {

	private final Path root = Paths.get("uploads");

	@Override
	public void init() {
		try {
			Files.createDirectory(root);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	@Override
	public void saveAndFindDuplicate(MultipartFile file) {
		try {
			Map<String, List<String>> antsMap = this.parseExcelFile(file.getInputStream());
		
			List<String> lstSpam = antsMap.get("spam");
			System.out.println("1. spam size [" + lstSpam.size() + "]" + System.lineSeparator());
			List<String> spamWithoutDupes = lstSpam.stream()
                    .distinct()
                    .collect(Collectors.toList());
			System.out.println("1.1 spam without duplicates size [" + spamWithoutDupes.size() + "]" + System.lineSeparator());
			List<String> spamWithoutDupesCommon = lstSpam.stream()
                    .distinct()
                    .collect(Collectors.toList());
			
			
			List<String> lstWhite = antsMap.get("whitelist");
			System.out.println("2. whitelist size ["+lstWhite.size()+"]" + System.lineSeparator());
			List<String> whiteWithoutDupes = lstWhite.stream()
                    .distinct()
                    .collect(Collectors.toList());
			System.out.println("2.1 whitelist without duplicates size [" + whiteWithoutDupes.size() + "]" + System.lineSeparator());
			
			spamWithoutDupes.retainAll(whiteWithoutDupes);
			
			System.out.println("Spam found in whitelist : " + spamWithoutDupes + ", size = "
					+ spamWithoutDupes.size() + System.lineSeparator());
			
			List<String> spamCommonDupes = spamWithoutDupesCommon.stream().filter(whiteWithoutDupes::contains).collect(Collectors.toList());
			System.out.println("Common >>> Sender Name spam in whitelist : " + spamCommonDupes + ", size = " + spamCommonDupes.size());   
			
			
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}
	
	
	//############################# EXCEL FUNCTION ########################################//
	private Map<String, List<String>> parseExcelFile(InputStream is) {
		try {
			Map<String, List<String>> map = new HashMap<String, List<String>>();
			// Spam list
			List<String> lstAntsSpam = new ArrayList<String>();
			// White list
			List<String> lstAntsWhitelist = new ArrayList<String>();
			
    		Workbook workbook = new XSSFWorkbook(is);
    		DataFormatter formatter = new DataFormatter();
    		Sheet s = workbook.getSheetAt(0);
    		//Sheet sheet = deleteEmptyRows(s);
    		Iterator<Row> rows = s.iterator();
    		int rowNumber = 0;
    		while (rows.hasNext()) {
    			Row currentRow = rows.next();
    			// skip header
    			if(rowNumber == 0) {
    				rowNumber++;
    				continue;
    			}
    			Iterator<Cell> cellsInRow = currentRow.iterator();
    			int cellIndex = 0;
    			while (cellsInRow.hasNext()) {
    				Cell currentCell = cellsInRow.next();
					if (cellIndex == 0) { // Spam
						lstAntsSpam.add(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 1) { // Whitelist
						lstAntsWhitelist.add(formatter.formatCellValue(currentCell));
					}
    				cellIndex++;
    			}

    			// set result
    			map.put("spam", lstAntsSpam);
    			map.put("whitelist", lstAntsWhitelist);
    			
    		}
    		// Close WorkBook
    		workbook.close();
    		return map;
        } catch (IOException e) {
        	throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
	}
	
	//############################# END EXCEL FUNCTION ##########################################//
		
}
