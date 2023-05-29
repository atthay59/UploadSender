package com.grokonez.excelfile.fileservice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.grokonez.excelfile.util.CDRExcelUtils;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.parsecsv.domains.CDRExport;
import com.parsecsv.domains.CDRInfo;
import com.parsecsv.domains.Cdr;

@Service
public class CsvStorageServiceImpl implements CsvStorageService {

	private final Path root = Paths.get("uploads");
	private final Path folderCdr = Paths.get("uploadsCdr");
	private static final Path folderFinalCdr = Paths.get("finalCDR");

	final static Pattern PATTERN = Pattern.compile("(.*?)(?:\\((\\d+)\\))?(\\.[^.]*)?");

	@Override
	public void init() {
		try {
			Files.createDirectory(root);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	//->>>>>>>>>>>>>>>>>>>>> CDR Version 1.0 <<<<<<<<<<<<<<<<<<<<-//
	@Override
	public void saveAndGroupCdr(MultipartFile file) {
		//temp
		List<Cdr> smsDRList = new ArrayList<Cdr>();
		List<Cdr> smsMTList = new ArrayList<Cdr>();
		
		try {
			// parse CSV file to create a list of `Cdr` objects
			Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
			
			System.out.println("parse CSV file to create a list of `Cdr` objects");
			// create csv bean reader		
			CsvToBean<Cdr> csvToBean = new CsvToBeanBuilder<Cdr>(reader)
                    .withType(Cdr.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
			System.out.println("create csv bean reader");
			
			// convert `CsvToBean` object to list of Cdr
			List<Cdr> cdrList = csvToBean.parse();
			System.out.println("convert `CsvToBean` object to list of Cdr");
			
			// Step 1) group MR/DR file upload.
			Map<String, List<Cdr>> lstCdrTypeGrpMap = this.groupByNotNullKey(cdrList, Cdr::getTypeShortMsg);
			System.out.println("Step 1) group MR/DR file upload.");
			
			// Step 2) initial to temp.
			for (Map.Entry<String, List<Cdr>> entry : lstCdrTypeGrpMap.entrySet()) {
				if ("SMS_DR".equals(entry.getKey())) {
					smsDRList = entry.getValue();
				} else {
					smsMTList = entry.getValue();
				}
			}
			System.out.println("Step 2) initial to temp.");
			
			// Step 3.1) DR group by Month > service id > count-indicator.
			Map<Object, Map<String, Map<String, List<Cdr>>>> multipleDRMapList = groupSMS(smsDRList);
			System.out.println("pass Step 3.1)");
			// Step 3.2) Add to CdrDRExport
			Map<String, List<CDRExport>> drExportList = prepareExportList(multipleDRMapList); 
			System.out.println("pass Step 3.2)");
			
			// Step 4.1) MT group by Month > service id > count-indicator.
			Map<Object, Map<String, Map<String, List<Cdr>>>> multipleMTMapList = groupSMS(smsMTList);
			System.out.println("pass Step 4.1)");
			// Step 4.2) Add to CdrDRExport
			Map<String, List<CDRExport>> mtExportList = prepareExportList(multipleMTMapList);
			System.out.println("pass Step 4.2)");
			
			// Step 5.1) export DR to excel
			ByteArrayInputStream inputStreamDR = CDRExcelUtils.exportToExcel(drExportList, "DR");
			String fileSuffixDR = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStreamDR, this.folderCdr.resolve("DR_" + fileSuffixDR + ".xlsx"));
			System.out.println("pass Step 5.1)");
			
			// Step 5.2) export MT to excel
			ByteArrayInputStream inputStreamMT = CDRExcelUtils.exportToExcel(mtExportList, "MT");
			String fileSuffixMT = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStreamMT, this.folderCdr.resolve("MT_" + fileSuffixMT + ".xlsx"));
			System.out.println("pass Step 5.2)");
			
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}
	
	private <E, K> Map<K, List<E>> groupByNotNullKey(List<E> list, Function<E, K> keyFunction) {
	    return Optional.ofNullable(list)
	            .orElseGet(ArrayList::new)
	            .stream()
	            .collect(Collectors.groupingBy(keyFunction));
	}
	
	// group by Month > service id > count-indicator.
	private Map<Object, Map<String, Map<String, List<Cdr>>>> groupSMS(List<Cdr> smsList) {
		Map<Object, Map<String, Map<String, List<Cdr>>>> multipleDRMapList = smsList.stream()
				.collect(
						Collectors.groupingBy(d -> YearMonth.from(d.getDateCharging()),
								Collectors.groupingBy(Cdr::getServiceId,
										Collectors.groupingBy(Cdr::getSuccessIndicator))));
		return multipleDRMapList;
	}
	
	private Map<String, List<CDRExport>> prepareExportList (Map<Object, Map<String, Map<String, List<Cdr>>>> multipleMapList) {
		Map<String, List<CDRExport>> exportMapList = new HashMap<String, List<CDRExport>>();
		// group of month
		for (Entry<Object, Map<String, Map<String, List<Cdr>>>> entryMonth : multipleMapList.entrySet()) {
			List<CDRExport> exportList = new ArrayList<CDRExport>();
			//drExport.setYearMonth(entryMonth.getKey().toString());
			// group of service id
			for (Entry<String, Map<String, List<Cdr>>> entryServiceId : entryMonth.getValue().entrySet()) {
				CDRExport drExport = new CDRExport();
				drExport.setServiceId(entryServiceId.getKey());
				// group of indicator
				int totalTrans = 0;
				for (Entry<String, List<Cdr>> entryIndicator : entryServiceId.getValue().entrySet()) {
					if ("Y".equals(entryIndicator.getKey())) {
						drExport.setSuccess(entryIndicator.getValue().size());
						totalTrans += entryIndicator.getValue().size();
					} else if ("N".equals(entryIndicator.getKey())) {
						drExport.setFail(entryIndicator.getValue().size());
						totalTrans += entryIndicator.getValue().size();
					}
				}
				drExport.setTotalTransection(totalTrans);
				exportList.add(drExport);
			}
			// Add to map
			exportMapList.put(entryMonth.getKey().toString(), exportList);
		}
		return exportMapList;
	}
	
	public List<File> getListCdrFiles() throws IOException {
		// gets the path uploadsCdr in workspace project
		String absolutePath = new File(".").getAbsolutePath();
		System.out.println(absolutePath);// Shows you the path of your Project Folder
		int last = absolutePath.length()-1;
		absolutePath = absolutePath.substring(0, last);//Remove the dot at the end of path
		System.out.println(absolutePath);
		String filePath =  "uploadsCdr";
		System.out.println(absolutePath + filePath); //Get the full path.
		String fullPath =  absolutePath + filePath;
		
		List<File> filesInFolder = Files.walk(Paths.get(fullPath))
	            .filter(Files::isRegularFile)
	            .map(Path::toFile)
	            .collect(Collectors.toList());
		
		return filesInFolder;
	}
	
	public static void main(String[] args)
    {
		try {
			List<File> filesInFolder = new CsvStorageServiceImpl().getListCdrFiles();
			
			System.out.println("CsvStorageServiceImpl.main(filesInFolder)" + filesInFolder);
			 
			List<File> drfiles = new ArrayList<>();
			List<File> mtfiles = new ArrayList<>();
			
			for (File file : filesInFolder) {
				if (file.getName().startsWith("DR")) {
					drfiles.add(file);
				} else if (file.getName().startsWith("MT")){
					mtfiles.add(file);
				}
			}
			System.out.println("CsvStorageServiceImpl.main(drfiles)" + drfiles);
			System.out.println("CsvStorageServiceImpl.main(mtfiles)" + mtfiles);
			
			Map<String, List<CDRExport>> drExportMapList = getExportMapList(drfiles);
			System.out.println("CsvStorageServiceImpl.main(drExportMapList)" + drExportMapList);
			
			Map<String, List<CDRExport>> mtExportMapList = getExportMapList(mtfiles);
			System.out.println("CsvStorageServiceImpl.main(mtExportMapList)" + mtExportMapList);
			
			// Step) export DR to excel
			ByteArrayInputStream inputStreamDR = CDRExcelUtils.exportToExcel(drExportMapList, "DR");
			String fileSuffixDR = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStreamDR, folderFinalCdr.resolve("DR_FINAL_" + fileSuffixDR + ".xlsx"));
			System.out.println("final step export DR)");
			
			// Step) export MT to excel
			ByteArrayInputStream inputStreamMT = CDRExcelUtils.exportToExcel(drExportMapList, "MT");
			String fileSuffixMT = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
			Files.copy(inputStreamMT, folderFinalCdr.resolve("MT_FINAL_" + fileSuffixMT + ".xlsx"));
			System.out.println("final step export MT)");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	public static Map<String, List<CDRExport>> getExportMapList(List<File> files) throws FileNotFoundException {
		// join all DR files 
		List<CDRInfo> allDRInfoList = new ArrayList<CDRInfo>();
		for (File file : files) {
			List<CDRInfo> crdInfoList = CDRExcelUtils.parseExcelFile(new FileInputStream(file));
			allDRInfoList.addAll(crdInfoList);
		}
		System.err.println(allDRInfoList);
		// Grouping result file
		Map<String, Map<String, List<CDRInfo>>> mapDR = allDRInfoList.stream()
				.collect(Collectors.groupingBy(CDRInfo::getYearMonth, Collectors.groupingBy(CDRInfo::getServiceId)));
		// Map to export
		Map<String, List<CDRExport>> exportMapList = new HashMap<String, List<CDRExport>>();
		// group of month
		for (Entry<String, Map<String, List<CDRInfo>>> entryMonth : mapDR.entrySet()) {
			List<CDRExport> exportList = new ArrayList<CDRExport>();
			// group of service id
			for (Entry<String, List<CDRInfo>> entryServiceId : entryMonth.getValue().entrySet()) {
				// looping sum int 
				int success = 0;
				int fail = 0;
				int total = 0;
				for (CDRInfo cdrInfo : entryServiceId.getValue()) {
					success += cdrInfo.getSuccess();
					fail += cdrInfo.getFail();
					total += cdrInfo.getTotalTransection();
				}
				CDRExport drExport = new CDRExport();
				drExport.setServiceId(entryServiceId.getKey());
				drExport.setSuccess(success);
				drExport.setFail(fail);
				drExport.setTotalTransection(total);
				exportList.add(drExport);
			}
			// Add to map
			exportMapList.put(entryMonth.getKey().toString(), exportList);
		}
		return exportMapList;
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
