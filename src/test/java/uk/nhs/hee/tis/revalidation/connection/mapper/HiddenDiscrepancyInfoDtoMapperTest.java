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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;

class HiddenDiscrepancyInfoDtoMapperTest {

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
  private static final LocalDate HIDDEN_UNTIL_1 =  LocalDate.now();
  private static final LocalDate HIDDEN_UNTIL_2 =  LocalDate.now().plusDays(1);

  private final HiddenDiscrepancyInfoMapper mapper =
      Mappers.getMapper(HiddenDiscrepancyInfoMapper.class);

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(mapper, "hiddenDiscrepancyMapper",
        new HiddenDiscrepancyMapperImpl());
  }

  @Test
  void shouldMapAllFields() {
    // given
    HiddenDiscrepancy hiddenDiscrepancy = HiddenDiscrepancy.builder()
        .id(GMC_ID1 + DBC1)
        .gmcId(GMC_ID1)
        .hiddenForDesignatedBodyCode(DBC1)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDate(HIDDEN_UNTIL_1)
        .build();

    MasterDoctorView doctorView = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID1)
        .designatedBody(DBC1)
        .tcsDesignatedBody(DBC2)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME1)
        .doctorLastName(LAST_NAME1)
        .hiddenDiscrepancies(List.of(hiddenDiscrepancy))
        .build();

    // when
    HiddenDiscrepancyInfoDto hiddenDiscrepancyInfoDto =
        mapper.toHiddenDiscrepancyInfoDto(doctorView);

    // then
    assertNotNull(hiddenDiscrepancyInfoDto);

    // mapped fields
    assertEquals(GMC_ID1,  hiddenDiscrepancyInfoDto.getGmcReferenceNumber());
    assertEquals(DBC1, hiddenDiscrepancyInfoDto.getDesignatedBody());
    assertEquals(DBC2, hiddenDiscrepancyInfoDto.getTcsDesignatedBody());
    assertEquals(PROGRAMME_NAME, hiddenDiscrepancyInfoDto.getProgrammeName());
    assertEquals(FIRST_NAME1, hiddenDiscrepancyInfoDto.getDoctorFirstName());
    assertEquals(LAST_NAME1, hiddenDiscrepancyInfoDto.getDoctorLastName());

    List<HiddenDiscrepancyDto> hiddenDiscrepancyDtos =
        hiddenDiscrepancyInfoDto.getHiddenDiscrepancies();
    assertEquals(1, hiddenDiscrepancyDtos.size());
    HiddenDiscrepancyDto hiddenDiscrepancyDto = hiddenDiscrepancyDtos.get(0);
    verifyHiddenDiscrepancyDto(hiddenDiscrepancyDto, hiddenDiscrepancy);
  }

  @Test
  void shouldMapAllFieldsInList() {
    // given
    HiddenDiscrepancy hiddenDiscrepancy1 = HiddenDiscrepancy.builder()
        .id(GMC_ID1 + DBC1)
        .gmcId(GMC_ID1)
        .hiddenForDesignatedBodyCode(DBC1)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDate(HIDDEN_UNTIL_1)
        .build();

    MasterDoctorView doctorView1 = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID1)
        .designatedBody(DBC1)
        .tcsDesignatedBody(DBC2)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME1)
        .doctorLastName(LAST_NAME1)
        .hiddenDiscrepancies(List.of(hiddenDiscrepancy1))
        .build();

    HiddenDiscrepancy hiddenDiscrepancy2 = HiddenDiscrepancy.builder()
        .id(GMC_ID2 + DBC2)
        .gmcId(GMC_ID2)
        .hiddenForDesignatedBodyCode(DBC2)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDate(HIDDEN_UNTIL_2)
        .build();

    MasterDoctorView doctorView2 = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_ID2)
        .designatedBody(DBC2)
        .tcsDesignatedBody(DBC1)
        .programmeName(PROGRAMME_NAME)
        .doctorFirstName(FIRST_NAME2)
        .doctorLastName(LAST_NAME2)
        .hiddenDiscrepancies(List.of(hiddenDiscrepancy2))
        .build();

    // when
    List<HiddenDiscrepancyInfoDto> list = mapper.toHiddenDiscrepancyInfoDtoList(
        List.of(doctorView1, doctorView2));

    // then
    assertEquals(2, list.size());

    // mapped fields
    var result1 = list.get(0);
    assertEquals(GMC_ID1, result1.getGmcReferenceNumber());
    assertEquals(DBC1, result1.getDesignatedBody());
    assertEquals(DBC2, result1.getTcsDesignatedBody());
    assertEquals(PROGRAMME_NAME, result1.getProgrammeName());
    assertEquals(FIRST_NAME1, result1.getDoctorFirstName());
    assertEquals(LAST_NAME1, result1.getDoctorLastName());
    assertEquals(1, result1.getHiddenDiscrepancies().size());
    verifyHiddenDiscrepancyDto(result1.getHiddenDiscrepancies().get(0), hiddenDiscrepancy1);

    var result2 = list.get(1);
    assertEquals(GMC_ID2, result2.getGmcReferenceNumber());
    assertEquals(DBC2, result2.getDesignatedBody());
    assertEquals(DBC1, result2.getTcsDesignatedBody());
    assertEquals(PROGRAMME_NAME, result2.getProgrammeName());
    assertEquals(FIRST_NAME2, result2.getDoctorFirstName());
    assertEquals(LAST_NAME2, result2.getDoctorLastName());
    assertEquals(1, result2.getHiddenDiscrepancies().size());
    verifyHiddenDiscrepancyDto(result2.getHiddenDiscrepancies().get(0), hiddenDiscrepancy2);
  }

  private void verifyHiddenDiscrepancyDto(HiddenDiscrepancyDto hiddenDiscrepancyDto,
      HiddenDiscrepancy hiddenDiscrepancy) {
    assertEquals(hiddenDiscrepancy.getId(), hiddenDiscrepancyDto.getId());
    assertEquals(hiddenDiscrepancy.getGmcId(), hiddenDiscrepancyDto.getGmcId());
    assertEquals(hiddenDiscrepancy.getHiddenForDesignatedBodyCode(),
        hiddenDiscrepancyDto.getHiddenForDesignatedBodyCode());
    assertEquals(hiddenDiscrepancy.getHiddenBy(), hiddenDiscrepancyDto.getHiddenBy());
    assertEquals(hiddenDiscrepancy.getReason(), hiddenDiscrepancyDto.getReason());
    assertEquals(hiddenDiscrepancy.getHiddenUntilDate(), hiddenDiscrepancyDto.getHiddenUntilDate());
  }
}
