package com.grokonez.excelfile.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.grokonez.excelfile.model.SenderName;
import com.parsecsv.domains.CDRExport;
import com.parsecsv.domains.CDRInfo;

public class CDRExcelUtils {

	public static ByteArrayInputStream exportToExcel(Map<String, List<CDRExport>> cdrExports, String Type) throws IOException {
		
		String COLUMNs[];
		
		if ("DR".equals(Type)) {
			COLUMNs = new String[] {"Year-Month", "Service ID", "DR Success", "DR Fail", "Total DR Transection"};
		} else {
			COLUMNs = new String[] {"Year-Month", "Service ID", "MT Success", "MT Fail", "Total MT Transection"};
		}
		try(
				Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
		){
			Sheet sheet = workbook.createSheet(Type);
	 
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
			/*CellStyle ageCellStyle = workbook.createCellStyle();
			ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));*/
	 
			int rowIdx = 1;
			int lastMergeRow = 0;
			for (Entry<String, List<CDRExport>> entry : cdrExports.entrySet()) {
				int startMergeRow = rowIdx;
				for (CDRExport c : entry.getValue()) {
					Row row = sheet.createRow(rowIdx++);
					
					row.createCell(0).setCellValue(entry.getKey());
					row.createCell(1).setCellValue(c.getServiceId());
					Integer iSuccess = c.getSuccess() == null ? 0 : c.getSuccess(); 
					row.createCell(2).setCellValue(iSuccess);
					Integer ifail = c.getFail() == null ? 0 : c.getFail(); 
					row.createCell(3).setCellValue(ifail);
					row.createCell(4).setCellValue(c.getTotalTransection());
				}
				
				// Merging cells by providing cell index
				int serviceIdSize = entry.getValue().size();
				lastMergeRow += serviceIdSize;
				
				CellRangeAddress ca = new CellRangeAddress(startMergeRow, lastMergeRow, 0, 0);
				sheet.addMergedRegion(ca);
				
				Cell cell = sheet.getRow(startMergeRow).getCell(0);
				CellStyle cellStyle = workbook.createCellStyle();
				cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
				cellStyle.setAlignment(HorizontalAlignment.CENTER);
				cell.setCellStyle(cellStyle);
			}
			
			// auto size
			/*sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);*/
	 
			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
	
	public static List<CDRInfo> parseExcelFile(InputStream is) {
		try {
    		Workbook workbook = new XSSFWorkbook(is);
    		DataFormatter formatter = new DataFormatter();
    		Sheet sheet = workbook.getSheetAt(0);
    		
    		Iterator<Row> rows = sheet.iterator();
    		
    		List<CDRInfo> lstCDRInfos = new ArrayList<CDRInfo>();
    		
    		int rowNumber = 0;
    		while (rows.hasNext()) {
    			Row currentRow = rows.next();
    			
    			// skip header
    			if(rowNumber == 0) {
    				rowNumber++;
    				continue;
    			}
    			Iterator<Cell> cellsInRow = currentRow.iterator();
    			
    			CDRInfo cdrInfo = new CDRInfo();
    			int cellIndex = 0;
    			while (cellsInRow.hasNext()) {
    				Cell currentCell = cellsInRow.next();
					if (cellIndex == 0) { // Year-Month
						cdrInfo.setYearMonth(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 1) { // Service ID
						cdrInfo.setServiceId(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 2) { // Success
						cdrInfo.setSuccess(Integer.parseInt(formatter.formatCellValue(currentCell)));
					} else if (cellIndex == 3) { // Fail
						cdrInfo.setFail(Integer.parseInt(formatter.formatCellValue(currentCell)));
					} else if (cellIndex == 4) { // total
						cdrInfo.setTotalTransection(Integer.parseInt(formatter.formatCellValue(currentCell)));
					}
    				cellIndex++;
    			}
    			lstCDRInfos.add(cdrInfo);
    		}
    		
    		// Close WorkBook
    		workbook.close();
    		
    		return lstCDRInfos;
        } catch (IOException e) {
        	throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
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
	
	
}