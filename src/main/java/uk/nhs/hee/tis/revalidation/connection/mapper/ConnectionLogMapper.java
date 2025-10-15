package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface ConnectionLogMapper {
  @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
  ConnectionLog fromDto(ConnectionLogDto connectionLogDto);

  ConnectionLogDto toDto(ConnectionLog connectionLog);
}
