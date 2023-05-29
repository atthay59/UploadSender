package com.grokonez.excelfile.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.grokonez.excelfile.model.AllowList;
import com.grokonez.excelfile.model.SenderName;

public class ExcelUtils {

	// New function 27-Sep-22
	public static ByteArrayInputStream allowListToExcel(List<AllowList> allowLists) throws IOException {
		String[] COLUMNs = {"SID", "List Sender Name", "Remark"};
		try(
				Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
		){
			CreationHelper createHelper = workbook.getCreationHelper();
	 
			Sheet sheet = workbook.createSheet("Allow list");
	 
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.BLUE.getIndex());
	 
			CellStyle headerCellStyle = workbook.createCellStyle();
			headerCellStyle.setFont(headerFont);
			headerCellStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
			headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	 
			// Row for Header
			Row headerRow = sheet.createRow(0);
	 
			// Header
			for (int col = 0; col < COLUMNs.length; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(COLUMNs[col]);
				cell.setCellStyle(headerCellStyle);
			}
	 
			// CellStyle for Age
			CellStyle ageCellStyle = workbook.createCellStyle();
			ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));
	 
			int rowIdx = 1;
			for (AllowList allow : allowLists) {
				Row row = sheet.createRow(rowIdx++);
	 
				row.createCell(0).setCellValue(allow.getSid());
				row.createCell(1).setCellValue(allow.getListSenderName());
				row.createCell(2).setCellValue(allow.getRemark());
	 
				Cell ageCell = row.createCell(2);
				ageCell.setCellValue(allow.getRemark());
				ageCell.setCellStyle(ageCellStyle);
			}
			
			/*int width = ((int)(maxNumCharacters * 1.14388)) * 256;
			sheet.setColumnWidth(i, width);*/
			
			// auto size
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
	 
			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
	
	public static ByteArrayInputStream senderNamesToExcel(List<SenderName> senderNames) throws IOException {
		String[] COLUMNs = {"MOBILE_NO", "CONTENT_PROVIDER_ID", "A_MSISDNRAW", "SERVICE_ID", "STATUS"};
		try(
				Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
		){
			CreationHelper createHelper = workbook.getCreationHelper();
	 
			Sheet sheet = workbook.createSheet("senderNameList");
	 
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.BLUE.getIndex());
	 
			CellStyle headerCellStyle = workbook.createCellStyle();
			headerCellStyle.setFont(headerFont);
	 
			// Row for Header
			Row headerRow = sheet.createRow(0);
	 
			// Header
			for (int col = 0; col < COLUMNs.length; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(COLUMNs[col]);
				cell.setCellStyle(headerCellStyle);
			}
	 
			// CellStyle for Age
			CellStyle ageCellStyle = workbook.createCellStyle();
			ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));
	 
			int rowIdx = 1;
			for (SenderName senderName : senderNames) {
				Row row = sheet.createRow(rowIdx++);
	 
				row.createCell(0).setCellValue(senderName.getMobileNo());
				row.createCell(1).setCellValue(senderName.getContentProviderId());
				row.createCell(2).setCellValue(senderName.getAmsisdnraw());
				row.createCell(3).setCellValue(senderName.getServiceId());
				row.createCell(4).setCellValue(senderName.getStatus());
	 
				Cell ageCell = row.createCell(3);
				ageCell.setCellValue(senderName.getServiceId());
				ageCell.setCellStyle(ageCellStyle);
			}
	 
			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
	
	public static List<SenderName> parseExcelFile(InputStream is) {
		try {
    		Workbook workbook = new XSSFWorkbook(is);
    		DataFormatter formatter = new DataFormatter();
    		Sheet s = workbook.getSheetAt(0);
    		
    		Sheet sheet = deleteEmptyRows(s);
    		
    		Iterator<Row> rows = sheet.iterator();
    		
    		List<SenderName> lstSenderNames = new ArrayList<SenderName>();
    		
    		int rowNumber = 0;
    		while (rows.hasNext()) {
    			Row currentRow = rows.next();
    			
    			// skip header
    			if(rowNumber == 0) {
    				rowNumber++;
    				continue;
    			}
    			
    			Iterator<Cell> cellsInRow = currentRow.iterator();

    			SenderName sender = new SenderName();
    			
    			int cellIndex = 0;
    			while (cellsInRow.hasNext()) {
    				Cell currentCell = cellsInRow.next();
					if (cellIndex == 0) { // MOBILE_NO
						sender.setMobileNo(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 1) { // CONTENT_PROVIDER_ID
						sender.setContentProviderId(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 2) { // A_MSISDNRAW
						sender.setAmsisdnraw(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 3) { // SERVICE_ID
						sender.setServiceId(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 4) { // STATUS
						sender.setStatus(formatter.formatCellValue(currentCell));
					}
    				cellIndex++;
    			}
    			
    			lstSenderNames.add(sender);
    		}
    		
    		// Close WorkBook
    		workbook.close();
    		
    		return lstSenderNames;
        } catch (IOException | InvalidFormatException e) {
        	throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
	}
	
	public static <E, K> Map<K, List<E>> groupByNotNullKey(List<E> list, Function<E, K> keyFunction) {
	    return Optional.ofNullable(list)
	            .orElseGet(ArrayList::new)
	            .stream()
	            .collect(Collectors.groupingBy(keyFunction));
	}
	
	public static Sheet deleteEmptyRows(Sheet sheet) throws InvalidFormatException, IOException {
		for (int r = sheet.getLastRowNum(); r >= 0; r--) {
			Row row = sheet.getRow(r);

			// if no row exists here; then nothing to do; next!
			if (row == null)
				continue;

			int lastColumn = row.getLastCellNum();
			boolean rowToDelete = true;
			if (lastColumn > -1) {
				for (int x = 0; x < lastColumn + 1; x++) {
					Cell cell = row.getCell(x);
					if (cell != null) {
						if (cell.getCellTypeEnum() == CellType.STRING) {
							if (StringUtils.isNotBlank(cell.getStringCellValue())) {
								rowToDelete = false;
								break;
							}
						}
						else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
							if (StringUtils.isNotBlank(String.valueOf(cell.getNumericCellValue()))) {
								rowToDelete = false;
								break;
							}
						}
					}
				}
			}

			if (rowToDelete) {
				if (r == sheet.getLastRowNum()) {
					sheet.removeRow(row);
				} else {
					sheet.removeRow(row);
					for (int j = r + 1; j <= sheet.getLastRowNum(); j++) {
						Row rowToShift = sheet.getRow(j);
						if (null != rowToShift)
							rowToShift.setRowNum(j - 1);
					}
				}
			}
		}
		return sheet;
	}
	
	public static void copyFile(File source, File dest) throws IOException, FileAlreadyExistsException {
		File[] children = source.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isFile() && !child.isHidden()) {

	                String lastEks = child.getName().toString();
	                StringBuilder b = new StringBuilder(lastEks);
	                File temp = new File(dest.toString() + "\\"
	                        + child.getName().toString());

	                if (child.getName().contains(".")) {
	                    if (temp.exists()) {
	                        temp = new File(dest.toString()
	                                + "\\"
	                                + b.replace(lastEks.lastIndexOf("."),
	                                        lastEks.lastIndexOf("."), " (1)")
	                                        .toString());
	                    } else {
	                        temp = new File(dest.toString() + "\\"
	                                + child.getName().toString());
	                    }
	                    b = new StringBuilder(temp.toString());
	                } else {
	                    temp = new File(dest.toString() + "\\"
	                            + child.getName());
	                }
	                if (temp.exists()) {
	                    for (int x = 1; temp.exists(); x++) {
	                        if (child.getName().contains(".")) {
	                            temp = new File(b.replace(
	                                    temp.toString().lastIndexOf(" "),
	                                    temp.toString().lastIndexOf("."),
	                                    " (" + x + ")").toString());
	                        } else {
	                            temp = new File(dest.toString() + "\\"
	                                    + child.getName() + " (" + x + ")");
	                        }
	                    }
	                    Files.copy(child.toPath(), temp.toPath());
	                } else {
	                    Files.copy(child.toPath(), temp.toPath());
	                }
	            } else if (child.isDirectory()) {
	                copyFile(child, dest);
	            }
	        }
	    }
	}
	
	public static void updateExistingExcelFile (String filePathString, List<SenderName> senderNames) {
		// Creating file object of existing excel file
        File xlsxFile = new File(filePathString);
        
        try {
            //Creating input stream
            FileInputStream inputStream = new FileInputStream(xlsxFile);
             
            //Creating workbook from input stream
            Workbook workbook = WorkbookFactory.create(inputStream);
 
            CreationHelper createHelper = workbook.getCreationHelper();
            
            //Reading first sheet of excel file
            Sheet sheet = workbook.getSheetAt(0);
 
            //Getting the count of existing records
            int rowCount = sheet.getLastRowNum();
            
			// CellStyle for Age
			CellStyle ageCellStyle = workbook.createCellStyle();
			ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));
         			
            //Iterating new students to update
            for (SenderName senderName : senderNames) {
                 
                //Creating new row from the next row count
                Row row = sheet.createRow(++rowCount);
 
                row.createCell(0).setCellValue(senderName.getMobileNo());
				row.createCell(1).setCellValue(senderName.getContentProviderId());
				row.createCell(2).setCellValue(senderName.getAmsisdnraw());
				row.createCell(3).setCellValue(senderName.getServiceId());
				row.createCell(4).setCellValue(senderName.getStatus());
	 
				Cell ageCell = row.createCell(3);
				ageCell.setCellValue(senderName.getServiceId());
				ageCell.setCellStyle(ageCellStyle);
            }
            //Close input stream
            inputStream.close();
 
            //Crating output stream and writing the updated workbook
            FileOutputStream os = new FileOutputStream(xlsxFile);
            workbook.write(os);
             
            //Close the workbook and output stream
            workbook.close();
            os.close();
             
            System.out.println("Excel file has been updated successfully. FileName : " + filePathString);
             
        } catch (EncryptedDocumentException | IOException | InvalidFormatException e) {
            System.err.println("Exception while updating an existing excel file.");
            e.printStackTrace();
        }
	}
	
	
}