package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface ConnectionLogMapper {
  @Mapping(source = "eventDateTime", target = "requestTime")
  ConnectionLog fromDto(ConnectionLogDto connectionLogDto);

  @Mapping(source = "requestTime", target = "eventDateTime")
  ConnectionLogDto toDto(ConnectionLog connectionLog);
}
