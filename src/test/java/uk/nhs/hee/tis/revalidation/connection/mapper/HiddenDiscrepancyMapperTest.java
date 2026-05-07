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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;

class HiddenDiscrepancyMapperTest {

  private static final String ID_1 = "aaaaaaaa11111111";
  private static final String ID_2 = "bbbbbbbb22222222";
  private static final String GMC_ID_1 = "1234567";
  private static final String GMC_ID_2 = "7654321";
  private static final String HIDDEN_FOR_DESIGNATED_BODY_CODE_1 = "1-ABCDEF";
  private static final String HIDDEN_FOR_DESIGNATED_BODY_CODE_2 = "1-GHIJKL";
  private static final String HIDDEN_BY_1 = "admin1";
  private static final String HIDDEN_BY_2 = "admin2";
  private static final String REASON_1 = "reason1";
  private static final String REASON_2 = "reason2";
  private static final LocalDateTime HIDDEN_DATE_TIME_1 = LocalDateTime.now();
  private static final LocalDateTime HIDDEN_DATE_TIME_2 = LocalDateTime.now().plusDays(1);

  private HiddenDiscrepancyMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new HiddenDiscrepancyMapperImpl();
  }

  @Test
  void shouldMapHiddenDiscrepancyToDto() {
    HiddenDiscrepancy hiddenDiscrepancy = HiddenDiscrepancy.builder()
        .id(ID_1)
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(HIDDEN_FOR_DESIGNATED_BODY_CODE_1)
        .hiddenBy(HIDDEN_BY_1)
        .reason(REASON_1)
        .hiddenDateTime(HIDDEN_DATE_TIME_1)
        .build();

    HiddenDiscrepancyDto dto = mapper.toHiddenDiscrepancyDto(hiddenDiscrepancy);

    assertNotNull(dto);
    assertEquals(ID_1, dto.getId());
    assertEquals(GMC_ID_1, dto.getGmcId());
    assertEquals(HIDDEN_FOR_DESIGNATED_BODY_CODE_1, dto.getHiddenForDesignatedBodyCode());
    assertEquals(HIDDEN_BY_1, dto.getHiddenBy());
    assertEquals(REASON_1, dto.getReason());
    assertEquals(HIDDEN_DATE_TIME_1, dto.getHiddenDateTime());
  }

  @Test
  void shouldMapHiddenDiscrepancyToDtoWithNullValues() {
    HiddenDiscrepancy hiddenDiscrepancy = HiddenDiscrepancy.builder()
        .id(ID_1)
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(HIDDEN_FOR_DESIGNATED_BODY_CODE_1)
        .hiddenBy(null)
        .reason(null)
        .hiddenDateTime(null)
        .build();

    HiddenDiscrepancyDto dto = mapper.toHiddenDiscrepancyDto(hiddenDiscrepancy);

    assertNotNull(dto);
    assertEquals(ID_1, dto.getId());
    assertEquals(GMC_ID_1, dto.getGmcId());
    assertEquals(HIDDEN_FOR_DESIGNATED_BODY_CODE_1, dto.getHiddenForDesignatedBodyCode());
    assertNull(dto.getHiddenBy());
    assertNull(dto.getReason());
    assertNull(dto.getHiddenDateTime());
  }

  @Test
  void shouldReturnNullWhenHiddenDiscrepancyIsNull() {
    HiddenDiscrepancyDto dto = mapper.toHiddenDiscrepancyDto(null);
    assertNull(dto);
  }

  @Test
  void shouldMapHiddenDiscrepancyListToDtoList() {
    HiddenDiscrepancy hiddenDiscrepancy1 = HiddenDiscrepancy.builder()
        .id(ID_1)
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(HIDDEN_FOR_DESIGNATED_BODY_CODE_1)
        .hiddenBy(HIDDEN_BY_1)
        .reason(REASON_1)
        .hiddenDateTime(HIDDEN_DATE_TIME_1)
        .build();

    HiddenDiscrepancy hiddenDiscrepancy2 = HiddenDiscrepancy.builder()
        .id(ID_2)
        .gmcId(GMC_ID_2)
        .hiddenForDesignatedBodyCode(HIDDEN_FOR_DESIGNATED_BODY_CODE_2)
        .hiddenBy(HIDDEN_BY_2)
        .reason(REASON_2)
        .hiddenDateTime(HIDDEN_DATE_TIME_2)
        .build();

    List<HiddenDiscrepancy> hiddenDiscrepancies = Arrays.asList(hiddenDiscrepancy1,
        hiddenDiscrepancy2);

    List<HiddenDiscrepancyDto> dtos = mapper.toHiddenDiscrepancyDtoList(hiddenDiscrepancies);

    assertNotNull(dtos);
    assertEquals(2, dtos.size());

    HiddenDiscrepancyDto dto1 = dtos.get(0);
    assertEquals(ID_1, dto1.getId());
    assertEquals(GMC_ID_1, dto1.getGmcId());
    assertEquals(HIDDEN_FOR_DESIGNATED_BODY_CODE_1, dto1.getHiddenForDesignatedBodyCode());
    assertEquals(HIDDEN_BY_1, dto1.getHiddenBy());
    assertEquals(REASON_1, dto1.getReason());
    assertEquals(HIDDEN_DATE_TIME_1, dto1.getHiddenDateTime());

    HiddenDiscrepancyDto dto2 = dtos.get(1);
    assertEquals(ID_2, dto2.getId());
    assertEquals(GMC_ID_2, dto2.getGmcId());
    assertEquals(HIDDEN_FOR_DESIGNATED_BODY_CODE_2, dto2.getHiddenForDesignatedBodyCode());
    assertEquals(HIDDEN_BY_2, dto2.getHiddenBy());
    assertEquals(REASON_2, dto2.getReason());
    assertEquals(HIDDEN_DATE_TIME_2, dto2.getHiddenDateTime());
  }

  @Test
  void shouldMapEmptyListToEmptyDtoList() {
    List<HiddenDiscrepancy> hiddenDiscrepancies = Collections.emptyList();

    List<HiddenDiscrepancyDto> dtos = mapper.toHiddenDiscrepancyDtoList(hiddenDiscrepancies);

    assertNotNull(dtos);
    assertTrue(dtos.isEmpty());
  }
}

