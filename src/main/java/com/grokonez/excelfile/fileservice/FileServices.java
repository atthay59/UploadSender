package com.grokonez.excelfile.fileservice;

import org.springframework.stereotype.Service;

@Service
public class FileServices{
	
	/*@Autowired
	CustomerRepository customerRepository;
	
	// Store File Data to Database
	public void store(MultipartFile file){
		try {
			List<Customer> lstCustomers = ExcelUtils.parseExcelFile(file.getInputStream());
			// Save Customers to DataBase
			customerRepository.saveAll(lstCustomers);
	    } catch (IOException e) {
	    	throw new RuntimeException("FAIL! -> message = " + e.getMessage());
	    }
	}
	
	// Load Data to Excel File
	public ByteArrayInputStream loadFile() {
		List<Customer> customers = (List<Customer>) customerRepository.findAll();
		
		try {
			ByteArrayInputStream in = ExcelUtils.customersToExcel(customers);
			return in;
		} catch (IOException e) {}
		
	    return null;
	}*/
}
