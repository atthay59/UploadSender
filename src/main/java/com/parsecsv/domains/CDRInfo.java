package com.parsecsv.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CDRInfo {

	private String yearMonth;

	private String serviceId;

	private Integer success;

	private Integer fail;

	private Integer totalTransection;

}
