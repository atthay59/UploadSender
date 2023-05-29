package com.parsecsv.domains;

import java.time.LocalDate;

import com.grokonez.excelfile.util.LocalDateConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Cdr {

	//@CsvDate(value = "yyyy-MM-dd")
	//@CsvBindByName(column = "datestartofcharging")
	@CsvCustomBindByName(column = "datestartofcharging", converter = LocalDateConverter.class)
    private LocalDate dateCharging;
	
    @CsvBindByName(column = "service_id")
    private String serviceId;
    
    @CsvBindByName(column = "type_of_short_message")
    private String typeShortMsg;
    
    @CsvBindByName(column = "a_msisdn")
    private String aMsisdn;
    
    @CsvBindByName(column = "b_msisdn")
    private String bMsisdn;
    
    @CsvBindByName(column = "c_msisdn")
    private String cMsisdn;

    @CsvBindByName(column = "calling_number")
    private String callingNumber;
    
    @CsvBindByName(column = "success_indicator")
    private String successIndicator;

    @CsvBindByName(column = "cause_for_termination")
    private String causeTermination;
    
    @CsvBindByName
    private String fragments;
    
    @CsvBindByName(column = "message_id")
    private String messageId;
    
}
