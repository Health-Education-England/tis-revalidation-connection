package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.ExceptionLogDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionLog;

@Mapper(componentModel = "spring")
public interface ExceptionLogMapper {

  @Mapping(target = "id", ignore = true)
  List<ExceptionLogDto> exceptionLogsToExceptionLogDtos(List<ExceptionLog> exceptionLog);
}
