/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;

class HiddenDiscrepancyDtoMapperTest {

  private static final String DBC1 = "DB123";
  private static final String DBC2 = "DB456";
  private static final String HIDDEN_BY = "admin";
  private static final String REASON = "reason for hiding";
  private static final String GMC_ID1 = "1111111";
  private static final String GMC_ID2 = "2222222";
  private static final String PROGRAMME_NAME = "programme";
  private static final String FIRST_NAME1 = "first1";
  private static final String FIRST_NAME2 = "first2";
  private static final String LAST_NAME1 = "last1";
  private static final String LAST_NAME2 = "last2";

  private final HiddenDiscrepancyInfoMapper mapper =
      Mappers.getMapper(HiddenDiscrepancyInfoMapper.class);

  @Test
  void shouldMapAllFields() {
    // given
    List<HiddenDiscrepancy> hiddenDiscrepancyList = List.of(
        HiddenDiscrepancy.builder()
            .id(GMC_ID1 + DBC1)
            .gmcId(GMC_ID1)
            .hiddenForDesignatedBodyCode(DBC1)
            .hiddenBy(HIDDEN_BY)
            .reason(REASON)
            .build()
    );

    MasterDoctorView doctorView = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID1)
        .designatedBody(DBC1)
        .tcsDesignatedBody(DBC2)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME1)
        .doctorLastName(LAST_NAME1)
        .hiddenDiscrepancies(hiddenDiscrepancyList)
        .build();

    // when
    HiddenDiscrepancyInfoDto hiddenDiscrepancyInfoDto =
        mapper.toHiddenDiscrepancyInfoDto(doctorView);

    // then
    assertThat(hiddenDiscrepancyInfoDto).isNotNull();

    // mapped fields
    assertThat(hiddenDiscrepancyInfoDto.getGmcReferenceNumber()).isEqualTo(GMC_ID1);
    assertThat(hiddenDiscrepancyInfoDto.getDesignatedBody()).isEqualTo(DBC1);
    assertThat(hiddenDiscrepancyInfoDto.getTcsDesignatedBody()).isEqualTo(DBC2);
    assertThat(hiddenDiscrepancyInfoDto.getHiddenDiscrepancies()).hasSize(1);
    assertThat(hiddenDiscrepancyInfoDto.getHiddenDiscrepancies()).isEqualTo(hiddenDiscrepancyList);
    assertThat(hiddenDiscrepancyInfoDto.getProgrammeName()).isEqualTo(PROGRAMME_NAME);
    assertThat(hiddenDiscrepancyInfoDto.getDoctorFirstName()).isEqualTo(FIRST_NAME1);
    assertThat(hiddenDiscrepancyInfoDto.getDoctorLastName()).isEqualTo(LAST_NAME1);
  }

  @Test
  void shouldMapAllFieldsInList() {
    // given
    List<HiddenDiscrepancy> hiddenDiscrepancyList1 = List.of(
        HiddenDiscrepancy.builder()
            .id(GMC_ID1 + DBC1)
            .gmcId(GMC_ID1)
            .hiddenForDesignatedBodyCode(DBC1)
            .hiddenBy(HIDDEN_BY)
            .reason(REASON)
            .build()
    );

    MasterDoctorView doctorView1 = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID1)
        .designatedBody(DBC1)
        .tcsDesignatedBody(DBC2)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME1)
        .doctorLastName(LAST_NAME1)
        .hiddenDiscrepancies(hiddenDiscrepancyList1)
        .build();

    List<HiddenDiscrepancy> hiddenDiscrepancyList2 = List.of(
        HiddenDiscrepancy.builder()
            .id(GMC_ID2 + DBC2)
            .gmcId(GMC_ID2)
            .hiddenForDesignatedBodyCode(DBC2)
            .hiddenBy(HIDDEN_BY)
            .reason(REASON)
            .build()
    );

    MasterDoctorView doctorView2 = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID2)
        .designatedBody(DBC2)
        .tcsDesignatedBody(DBC1)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME2)
        .doctorLastName(LAST_NAME2)
        .hiddenDiscrepancies(hiddenDiscrepancyList2)
        .build();

    // when
    List<HiddenDiscrepancyInfoDto> list = mapper.toHiddenDiscrepancyInfoDtoList(
        List.of(doctorView1, doctorView2));

    // then
    assertThat(list).hasSize(2);

    // mapped fields
    var result1 = list.get(0);
    var result2 = list.get(1);

    assertThat(result1.getGmcReferenceNumber()).isEqualTo(GMC_ID1);
    assertThat(result1.getDesignatedBody()).isEqualTo(DBC1);
    assertThat(result1.getTcsDesignatedBody()).isEqualTo(DBC2);
    assertThat(result1.getHiddenDiscrepancies()).hasSize(1);
    assertThat(result1.getHiddenDiscrepancies()).isEqualTo(hiddenDiscrepancyList1);
    assertThat(result1.getProgrammeName()).isEqualTo(PROGRAMME_NAME);
    assertThat(result1.getDoctorFirstName()).isEqualTo(FIRST_NAME1);
    assertThat(result1.getDoctorLastName()).isEqualTo(LAST_NAME1);

    assertThat(result2.getGmcReferenceNumber()).isEqualTo(GMC_ID2);
    assertThat(result2.getDesignatedBody()).isEqualTo(DBC2);
    assertThat(result2.getTcsDesignatedBody()).isEqualTo(DBC1);
    assertThat(result2.getHiddenDiscrepancies()).hasSize(1);
    assertThat(result2.getHiddenDiscrepancies()).isEqualTo(hiddenDiscrepancyList2);
    assertThat(result2.getProgrammeName()).isEqualTo(PROGRAMME_NAME);
    assertThat(result2.getDoctorFirstName()).isEqualTo(FIRST_NAME2);
    assertThat(result2.getDoctorLastName()).isEqualTo(LAST_NAME2);
  }
}
