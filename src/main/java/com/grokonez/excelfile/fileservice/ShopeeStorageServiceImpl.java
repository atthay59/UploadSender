package com.grokonez.excelfile.fileservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.grokonez.excelfile.model.SenderName;
import com.grokonez.excelfile.model.ShopeeGroup;
import com.grokonez.excelfile.model.ShopeeSIDList;
import com.grokonez.excelfile.util.ExcelUtils;

@Service
public class ShopeeStorageServiceImpl implements ShopeeStorageService {

	private final Path root = Paths.get("uploads");
	private final Path folderShopee = Paths.get("uploadsShopee");

	final static Pattern PATTERN = Pattern.compile("(.*?)(?:\\((\\d+)\\))?(\\.[^.]*)?");

	@Override
	public void init() {
		try {
			Files.createDirectory(root);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	@Override
	public void saveAndGroupShopee(MultipartFile file) {
		try {
			List<ShopeeGroup> lstShopeeGroups = this.parseExcelFile(file.getInputStream());

			System.out.println("lstShopeeGroups step 1." + System.lineSeparator());
			
			/*Shopee
			ShopeePay
			SPayLater
			ShopeeFood
			SeaMoney
			ShopeeX_TH*/
			
			Map<String, String> shopeeMap = new HashMap<String, String>();
			
			//Case Shopee
			String shopee = this.filterShopee(lstShopeeGroups, "Shopee");
			shopeeMap.put("Shopee", shopee);
			System.out.println("Shopee : " + shopee + System.lineSeparator());
			
			//Case ShopeePay
			String shopeePay = this.filterShopee(lstShopeeGroups, "ShopeePay");
			shopeeMap.put("ShopeePay", shopeePay);
			System.out.println("ShopeePay : " + shopeePay + System.lineSeparator());
			
			//Case SPayLater
			String sPayLater = this.filterShopee(lstShopeeGroups, "SPayLater");
			shopeeMap.put("SPayLater", sPayLater);
			System.out.println("SPayLater : " + sPayLater + System.lineSeparator());
			
			//Case ShopeePay
			String shopeeFood = this.filterShopee(lstShopeeGroups, "ShopeeFood");
			shopeeMap.put("ShopeeFood", shopeeFood);
			System.out.println("ShopeeFood : " + shopeeFood + System.lineSeparator());
			
			//Case SeaMoney
			String seaMoney = this.filterShopee(lstShopeeGroups, "SeaMoney");
			shopeeMap.put("SeaMoney", seaMoney);
			System.out.println("SeaMoney : " + seaMoney + System.lineSeparator());
			
			//Case ShopeeX_TH
			String shopeeX_TH = this.filterShopee(lstShopeeGroups, "ShopeeX_TH");
			shopeeMap.put("ShopeeX_TH", shopeeX_TH);
			System.out.println("ShopeeX_TH : " + shopeeX_TH + System.lineSeparator());
			
			//System.out.println("shopeeMap step " + shopeeMap);
			
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}
	
	private String filterShopee (List<ShopeeGroup> lstShopeeGroups, String keyword) {
		StringBuilder shopeeBuilder = new StringBuilder();
		Instant start = Instant.now();
		System.out.println("split start");
		for (ShopeeGroup s : lstShopeeGroups) {
			//Shopee case
			/*if (StringUtils.contains(s.getSenderName(), keyword)) {
			}*/
			
			List<String> senderNameitems = Stream.of(s.getSenderName().split(","))
				     .map(String::trim)
				     .collect(Collectors.toList());
			for (String sender : senderNameitems) {
				if (sender.equals(keyword)) {
					if (shopeeBuilder.length() == 0) {
						shopeeBuilder.append(s.getServiceId());
					} else {
						//Not duplicate
						if (!shopeeBuilder.toString().contains(s.getServiceId())) {
							shopeeBuilder.append("," + s.getServiceId());
						}
					}
				}
			}
		}
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		System.out.println("split end " + timeElapsed);
		return shopeeBuilder.toString();
	}
	
	
	
	
	//############################# EXCEL FUNCTION ########################################//
	private List<ShopeeGroup> parseExcelFile(InputStream is) {
		try {
    		Workbook workbook = new XSSFWorkbook(is);
    		DataFormatter formatter = new DataFormatter();
    		Sheet s = workbook.getSheetAt(0);
    		//Sheet sheet = deleteEmptyRows(s);
    		Iterator<Row> rows = s.iterator();
    		List<ShopeeGroup> lstShopeeGroups = new ArrayList<ShopeeGroup>();
    		int rowNumber = 0;
    		while (rows.hasNext()) {
    			Row currentRow = rows.next();
    			// skip header
    			if(rowNumber == 0) {
    				rowNumber++;
    				continue;
    			}
    			Iterator<Cell> cellsInRow = currentRow.iterator();
    			ShopeeGroup shopee = new ShopeeGroup();
    			int cellIndex = 0;
    			while (cellsInRow.hasNext()) {
    				Cell currentCell = cellsInRow.next();
					if (cellIndex == 0) { // CP_Name_ServiceID
						shopee.setServiceId(formatter.formatCellValue(currentCell));
					} else if (cellIndex == 1) { // Sender_Name
						shopee.setSenderName(formatter.formatCellValue(currentCell));
					}
    				cellIndex++;
    			}
    			lstShopeeGroups.add(shopee);
    		}
    		// Close WorkBook
    		workbook.close();
    		return lstShopeeGroups;
        } catch (IOException e) {
        	throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
	}
	
	private ByteArrayInputStream shopeeListToExcel(List<ShopeeSIDList> shopeeSIDLists) throws IOException {
		String[] COLUMNs = { "SID", "List Sender Name"};
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			CreationHelper createHelper = workbook.getCreationHelper();
			Sheet sheet = workbook.createSheet("Shopee List");
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
			for (ShopeeSIDList s : shopeeSIDLists) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(s.getSid());
				row.createCell(1).setCellValue(s.getSenderName());
				Cell sidCell = row.createCell(0);
				sidCell.setCellValue(s.getSid());
				sidCell.setCellStyle(ageCellStyle);
			}
			// auto size
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
	
	//############################# END EXCEL FUNCTION ##########################################//
		
		
	@Override
	public void save(MultipartFile file) {
		try {
			List<SenderName> lstSenderNames = ExcelUtils.parseExcelFile(file.getInputStream());

			Map<String, List<SenderName>> lstSerderGrpMap = ExcelUtils.groupByNotNullKey(lstSenderNames,
					SenderName::getServiceId);

			for (Map.Entry<String, List<SenderName>> entry : lstSerderGrpMap.entrySet()) {
				//System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
				List<SenderName> lstSenderGroups = entry.getValue();

				//Test update existing version
				String filePathString = this.root.resolve(entry.getKey() + ".xlsx").toString();
				Path source = Paths.get(filePathString);
				if (Files.exists(source)) {
					ExcelUtils.updateExistingExcelFile(filePathString, lstSenderGroups);
				} else {
					ByteArrayInputStream inputStream = ExcelUtils.senderNamesToExcel(lstSenderGroups);
					Files.copy(inputStream, this.root.resolve(entry.getKey() + ".xlsx"));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}

	public String getNewName(String filename) {
		String filePathString = this.root.resolve(filename).toString();
		Path source = Paths.get(filePathString);

		String filePathNewName = "";
		Path newSource;
		if (Files.exists(source)) {
			Matcher m = PATTERN.matcher(filename);
			if (m.matches()) {
				String prefix = m.group(1);
				String last = m.group(2);
				String suffix = m.group(3);
				if (suffix == null)
					suffix = "";

				int count = last != null ? Integer.parseInt(last) : 0;

				do {
					count++;
					filename = prefix + "(" + count + ")" + suffix;
					filePathNewName = this.root.resolve(filename).toString();
					newSource = Paths.get(filePathNewName);
				} while (Files.exists(newSource));
			}
		}
		return filename;
	}

	@Override
	public Resource load(String filename) {
		try {
			Path file = root.resolve(filename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new RuntimeException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(root.toFile());
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}
	
	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) 
	{
	    Map<Object, Boolean> map = new ConcurrentHashMap<>();
	    return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

}
