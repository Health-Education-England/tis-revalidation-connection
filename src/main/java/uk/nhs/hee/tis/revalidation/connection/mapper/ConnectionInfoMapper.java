/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;

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

  @Mapping(target = "programmeMembershipType", source = "membershipType")
  @Mapping(target = "programmeMembershipStartDate", source = "membershipStartDate")
  @Mapping(target = "programmeMembershipEndDate", source = "membershipEndDate")
  @Mapping(target = "dataSource", ignore = true)
  ConnectionInfoDto masterToDto(MasterDoctorView userType);

  List<ConnectionInfoDto> masterToDtos(List<MasterDoctorView> userTypes);

  List<ConnectionInfoDto> masterToDtos(Iterable<MasterDoctorView> userTypes);

  @Mapping(target = "membershipType", source = "programmeMembershipType")
  @Mapping(target = "membershipStartDate", source = "programmeMembershipStartDate")
  @Mapping(target = "membershipEndDate", source = "programmeMembershipEndDate")
  @Mapping(target = "id", ignore = true)
  MasterDoctorView dtoToMaster(ConnectionInfoDto connectionInfoDto);
}
