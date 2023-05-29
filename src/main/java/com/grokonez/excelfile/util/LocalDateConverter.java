package com.grokonez.excelfile.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

@SuppressWarnings("rawtypes")
public class LocalDateConverter extends AbstractBeanField {

	@Override
    protected Object convert(String s) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dt = StringUtils.truncate(s, 10); //ex. 2023-03-20 22:18:21
        LocalDate parse = LocalDate.parse(dt, formatter);
        return parse;
    }
	
}
