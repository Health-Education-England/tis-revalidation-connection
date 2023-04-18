/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.LocalDate;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.CurrentConnectionsView;
import uk.nhs.hee.tis.revalidation.connection.entity.DiscrepanciesView;

class ConnectionInfoMapperTest {

  private static final String GMC_NUMBER = "gmc_number";
  private static final String DOCTOR_FIRST_NAME = "first_name";
  private static final String DOCTOR_LAST_NAME = "last_name";
  private static final String DESIGNATED_BODY = "designated_body";
  private static final String TCS_DESIGNATED_BODY = "tcs_designated_body";
  private static final Long TCS_PERSON_ID = 1L;
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "memberhip_type";
  private static final LocalDate PROGRAMME_MEMBERSHIP_START_DATE = LocalDate.now();
  private static final LocalDate PROGRAMME_MEMBERSHIP_END_DATE = LocalDate.now().plusDays(1);
  private static final String EXCEPTION_REASON = "exception_reason";

  ConnectionInfoMapper testObj = new ConnectionInfoMapperImpl();

  @Test
  void shouldMapCurrentConnectionViewListToConnectionInfoDtoList() {

    CurrentConnectionsView currentConnectionsView = CurrentConnectionsView.builder()
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME)
        .doctorLastName(DOCTOR_LAST_NAME)
        .designatedBody(DESIGNATED_BODY)
        .tcsDesignatedBody(TCS_DESIGNATED_BODY)
        .tcsPersonId(TCS_PERSON_ID)
        .membershipType(PROGRAMME_MEMBERSHIP_TYPE)
        .membershipStartDate(PROGRAMME_MEMBERSHIP_START_DATE)
        .membershipEndDate(PROGRAMME_MEMBERSHIP_END_DATE)
        .exceptionReason(EXCEPTION_REASON)
        .build();

    List<ConnectionInfoDto> connectionInfoDtos =
        testObj.currentConnectionsToConnectionInfoDtos(Lists.list(currentConnectionsView));

    assertThat(connectionInfoDtos.size(), is(1));
    ConnectionInfoDto dto = connectionInfoDtos.get(0);
    assertThat(dto.getGmcReferenceNumber(), is(GMC_NUMBER));
    assertThat(dto.getDoctorFirstName(), is(DOCTOR_FIRST_NAME));
    assertThat(dto.getDoctorLastName(), is(DOCTOR_LAST_NAME));
    assertThat(dto.getDesignatedBody(), is(DESIGNATED_BODY));
    assertThat(dto.getTcsDesignatedBody(), is(TCS_DESIGNATED_BODY));
    assertThat(dto.getTcsPersonId(), is(TCS_PERSON_ID));
    assertThat(dto.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat(dto.getProgrammeMembershipStartDate(), is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat(dto.getProgrammeMembershipEndDate(), is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat(dto.getExceptionReason(), is(EXCEPTION_REASON));
  }

  @Test
  void shouldMapDiscrepanciesViewListToConnectionInfoDtoList() {

    DiscrepanciesView discrepanciesView = DiscrepanciesView.builder()
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME)
        .doctorLastName(DOCTOR_LAST_NAME)
        .designatedBody(DESIGNATED_BODY)
        .tcsDesignatedBody(TCS_DESIGNATED_BODY)
        .tcsPersonId(TCS_PERSON_ID)
        .membershipType(PROGRAMME_MEMBERSHIP_TYPE)
        .membershipStartDate(PROGRAMME_MEMBERSHIP_START_DATE)
        .membershipEndDate(PROGRAMME_MEMBERSHIP_END_DATE)
        .exceptionReason(EXCEPTION_REASON)
        .build();

    List<ConnectionInfoDto> connectionInfoDtos =
        testObj.discrepancyToConnectionInfoDtos(Lists.list(discrepanciesView));

    assertThat(connectionInfoDtos.size(), is(1));
    ConnectionInfoDto dto = connectionInfoDtos.get(0);
    assertThat(dto.getGmcReferenceNumber(), is(GMC_NUMBER));
    assertThat(dto.getDoctorFirstName(), is(DOCTOR_FIRST_NAME));
    assertThat(dto.getDoctorLastName(), is(DOCTOR_LAST_NAME));
    assertThat(dto.getDesignatedBody(), is(DESIGNATED_BODY));
    assertThat(dto.getTcsDesignatedBody(), is(TCS_DESIGNATED_BODY));
    assertThat(dto.getTcsPersonId(), is(TCS_PERSON_ID));
    assertThat(dto.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat(dto.getProgrammeMembershipStartDate(), is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat(dto.getProgrammeMembershipEndDate(), is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat(dto.getExceptionReason(), is(EXCEPTION_REASON));
  }
}
