package com.grokonez.excelfile.fileservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.grokonez.excelfile.model.AllowList;
import com.grokonez.excelfile.model.SenderName;
import com.grokonez.excelfile.model.WhiteListExport;
import com.grokonez.excelfile.model.WhiteListSpam;
import com.grokonez.excelfile.util.ExcelUtils;
import com.grokonez.excelfile.util.SpamExcelUtils;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

	private final Path root = Paths.get("uploads");
	private final Path folderSpam = Paths.get("uploadsSpam");

	final static Pattern PATTERN = Pattern.compile("(.*?)(?:\\((\\d+)\\))?(\\.[^.]*)?");

	@Override
	public void init() {
		try {
			Files.createDirectory(root);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	// new function
	@Override
	public void saveAndGroupAmsisdnraw(MultipartFile file) {
		try {
			List<SenderName> lstSenderNames = ExcelUtils.parseExcelFile(file.getInputStream());

			// Step 1) group all excel file upload. 
			Map<String, List<SenderName>> lstSerderGrpMap = ExcelUtils.groupByNotNullKey(lstSenderNames,
					SenderName::getServiceId);

			List<AllowList> allowLists = new ArrayList<AllowList>(); 
			// Step 2) merge sender name to allow list.
			for (Map.Entry<String, List<SenderName>> entry : lstSerderGrpMap.entrySet()) {
				List<SenderName> lstSenderGroups = entry.getValue();
				StringBuilder joinSenderName = new StringBuilder();
				for (SenderName senderName : lstSenderGroups) {
					joinSenderName.append(",".concat(senderName.getAmsisdnraw()));
				} 
				AllowList allow = new AllowList();
				String sid = entry.getKey();
				String remark = lstSenderGroups.get(0).getContentProviderId();
				allow.setListSenderName(joinSenderName.toString());
				allow.setSid(sid);
				allow.setRemark(remark);
				allowLists.add(allow);
			}
			
			// Step 3) export allowlist to excel
			ByteArrayInputStream inputStream = ExcelUtils.allowListToExcel(allowLists);
			String fileSuffix = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStream, this.root.resolve("Allow_List_" + fileSuffix + ".xlsx"));
			
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}

	
	//->>>>>>>>>>>>>>>>>>>>> SPAM Version 1.2 <<<<<<<<<<<<<<<<<<<<-//
	// new function
	@Override
	public void saveAndGroupSpam(MultipartFile file) {
		try {
			List<WhiteListSpam> lstWhiteListSpam = SpamExcelUtils.parseExcelFile(file.getInputStream());
			
			// Step 1) group all excel file upload. 
			Map<String, List<WhiteListSpam>> lstSpamGrpMap = SpamExcelUtils.groupByNotNullKey(lstWhiteListSpam,
					WhiteListSpam::getCustomerNameEn);
			
			// Export Object
			List<WhiteListExport> whiteListExport = new ArrayList<WhiteListExport>();
			
			// Step 2) remove duplicates from a lstSpamGrpMap
			for (Map.Entry<String, List<WhiteListSpam>> entry : lstSpamGrpMap.entrySet()) {
				List<WhiteListSpam> lstSpam = entry.getValue();
				
				// Get distinct objects by key
				List<WhiteListSpam> lstSpamDistinct = lstSpam.stream()
						.filter( distinctByKey(p -> p.getSenderName()) )
						.collect( Collectors.toList() );
				// Add to WhiteListExport
				for (WhiteListSpam spamDistinct : lstSpamDistinct) {
					WhiteListExport wExport = new WhiteListExport();
					wExport.setCustomerNameTh(spamDistinct.getCustomerName());
					wExport.setCustomerNameEn(spamDistinct.getCustomerNameEn());
					wExport.setSenderName(spamDistinct.getSenderName());
					wExport.setCreatedDate(spamDistinct.getCreatedDate());
					whiteListExport.add(wExport);
				}
			}
			
			// Step 3) export allowlist to excel
			ByteArrayInputStream inputStream = SpamExcelUtils.whitelistToExcel(whiteListExport);
			String fileSuffix = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStream, this.folderSpam.resolve("White_List_" + fileSuffix + ".xlsx"));
			
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}
	
	
	@Override
	public void save(MultipartFile file) {
		try {
			List<SenderName> lstSenderNames = ExcelUtils.parseExcelFile(file.getInputStream());

			Map<String, List<SenderName>> lstSerderGrpMap = ExcelUtils.groupByNotNullKey(lstSenderNames,
					SenderName::getServiceId);

			for (Map.Entry<String, List<SenderName>> entry : lstSerderGrpMap.entrySet()) {
				//System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
				List<SenderName> lstSenderGroups = entry.getValue();

				//Success version 1 rename file
				/*ByteArrayInputStream inputStream = ExcelUtils.senderNamesToExcel(lstSenderGroups);
				String name = getNewName(entry.getKey() + ".xlsx");
				Files.copy(inputStream, this.root.resolve(name));*/

				//Test update existing version
				String filePathString = this.root.resolve(entry.getKey() + ".xlsx").toString();
				Path source = Paths.get(filePathString);
				if (Files.exists(source)) {
					ExcelUtils.updateExistingExcelFile(filePathString, lstSenderGroups);
				} else {
					ByteArrayInputStream inputStream = ExcelUtils.senderNamesToExcel(lstSenderGroups);
					Files.copy(inputStream, this.root.resolve(entry.getKey() + ".xlsx"));
				}

				// Save Customers to local store
				// String filePathString = this.root.resolve(entry.getKey()+".xlsx").toString();
				/*Path source = Paths.get(filePathString);
				if (Files.exists(source)) {
					Format formatter = new SimpleDateFormat("YYYY-MM-dd_hh-mm-ss");
					String newName = entry.getKey() + formatter.format(new Date()) + ".xlsx";
				    Files.move(source, source.resolveSibling(newName), StandardCopyOption.REPLACE_EXISTING);
				} else {
					Files.copy(inputStream, this.root.resolve(entry.getKey()+".xlsx"));
				}*/
				//File source =  new File(filePathString);
				//ExcelUtils.copyFile(null, null);
			}

			// Save Customers to local store
			//Files.copy(file.getInputStream(), this.root.resolve(file.getOriginalFilename()));
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
