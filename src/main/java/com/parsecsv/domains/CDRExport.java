package com.parsecsv.domains;

import lombok.Data;

@Data
public class CDRExport {
	
	//private String yearMonth;
	
	private String serviceId;
	
	private Integer success;
	
	private Integer fail;
	
	private Integer totalTransection;
 
}