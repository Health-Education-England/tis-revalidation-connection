package uk.nhs.hee.tis.revalidation.connection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConnectionInfoMapper {

  @Mapping(target = "programmeMembershipType", source = "membershipType")
  @Mapping(target = "programmeMembershipStartDate", source = "membershipStartDate")
  @Mapping(target = "programmeMembershipEndDate", source = "membershipEndDate")
  @Mapping(target = "dataSource", ignore = true)
  ConnectionInfoDto exceptionToDto(ExceptionView userType);

  List<ConnectionInfoDto> exceptionToDtos(List<ExceptionView> userTypes);

  @Mapping(target = "programmeMembershipType", source = "membershipType")
  @Mapping(target = "programmeMembershipStartDate", source = "membershipStartDate")
  @Mapping(target = "programmeMembershipEndDate", source = "membershipEndDate")
  @Mapping(target = "dataSource", ignore = true)
  ConnectionInfoDto connectedToDto(ConnectedView userType);

  List<ConnectionInfoDto> connectedToDtos(List<ConnectedView> userTypes);

  @Mapping(target = "programmeMembershipType", source = "membershipType")
  @Mapping(target = "programmeMembershipStartDate", source = "membershipStartDate")
  @Mapping(target = "programmeMembershipEndDate", source = "membershipEndDate")
  @Mapping(target = "dataSource", ignore = true)
  ConnectionInfoDto disconnectedToDto(DisconnectedView userType);

  List<ConnectionInfoDto> disconnectedToDtos(List<DisconnectedView> userTypes);
}
