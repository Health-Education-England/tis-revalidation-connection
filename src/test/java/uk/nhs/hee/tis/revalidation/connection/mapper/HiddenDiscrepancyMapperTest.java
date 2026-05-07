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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    assertThat(dto.getId(), is(ID_1));
    assertThat(dto.getGmcId(), is(GMC_ID_1));
    assertThat(dto.getHiddenForDesignatedBodyCode(), is(HIDDEN_FOR_DESIGNATED_BODY_CODE_1));
    assertThat(dto.getHiddenBy(), is(HIDDEN_BY_1));
    assertThat(dto.getReason(), is(REASON_1));
    assertThat(dto.getHiddenDateTime(), is(HIDDEN_DATE_TIME_1));
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
    assertThat(dto.getId(), is(ID_1));
    assertThat(dto.getGmcId(), is(GMC_ID_1));
    assertThat(dto.getHiddenForDesignatedBodyCode(), is(HIDDEN_FOR_DESIGNATED_BODY_CODE_1));
    assertThat(dto.getHiddenBy(), is(nullValue()));
    assertThat(dto.getReason(), is(nullValue()));
    assertThat(dto.getHiddenDateTime(), is(nullValue()));
  }

  @Test
  void shouldReturnNullWhenHiddenDiscrepancyIsNull() {
    HiddenDiscrepancyDto dto = mapper.toHiddenDiscrepancyDto(null);
    assertThat(dto, is(nullValue()));
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

    List<HiddenDiscrepancy> hiddenDiscrepancies = Arrays.asList(hiddenDiscrepancy1, hiddenDiscrepancy2);

    List<HiddenDiscrepancyDto> dtos = mapper.toHiddenDiscrepancyDtoList(hiddenDiscrepancies);

    assertNotNull(dtos);
    assertThat(dtos.size(), is(2));

    HiddenDiscrepancyDto dto1 = dtos.get(0);
    assertThat(dto1.getId(), is(ID_1));
    assertThat(dto1.getGmcId(), is(GMC_ID_1));
    assertThat(dto1.getHiddenForDesignatedBodyCode(), is(HIDDEN_FOR_DESIGNATED_BODY_CODE_1));
    assertThat(dto1.getHiddenBy(), is(HIDDEN_BY_1));
    assertThat(dto1.getReason(), is(REASON_1));
    assertThat(dto1.getHiddenDateTime(), is(HIDDEN_DATE_TIME_1));

    HiddenDiscrepancyDto dto2 = dtos.get(1);
    assertThat(dto2.getId(), is(ID_2));
    assertThat(dto2.getGmcId(), is(GMC_ID_2));
    assertThat(dto2.getHiddenForDesignatedBodyCode(), is(HIDDEN_FOR_DESIGNATED_BODY_CODE_2));
    assertThat(dto2.getHiddenBy(), is(HIDDEN_BY_2));
    assertThat(dto2.getReason(), is(REASON_2));
    assertThat(dto2.getHiddenDateTime(), is(HIDDEN_DATE_TIME_2));
  }

  @Test
  void shouldMapEmptyListToEmptyDtoList() {
    List<HiddenDiscrepancy> hiddenDiscrepancies = Collections.emptyList();

    List<HiddenDiscrepancyDto> dtos = mapper.toHiddenDiscrepancyDtoList(hiddenDiscrepancies);

    assertNotNull(dtos);
    assertThat(dtos.size(), is(0));
  }
}

