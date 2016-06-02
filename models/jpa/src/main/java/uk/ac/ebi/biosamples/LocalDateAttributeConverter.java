package uk.ac.ebi.biosamples;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * This class is needed to map a Java 8 LocalDate to a SQL Date object rather than a blob
 * 
 * @author faulcon
 *
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

	@Override
	public Date convertToDatabaseColumn(LocalDate locDate) {
		return (locDate == null ? null : Date.valueOf(locDate));
	}

	@Override
	public LocalDate convertToEntityAttribute(Date sqlDate) {
		return (sqlDate == null ? null : sqlDate.toLocalDate());
	}
}