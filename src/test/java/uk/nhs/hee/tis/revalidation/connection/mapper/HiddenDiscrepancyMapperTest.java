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

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;

class HiddenDiscrepancyMapperTest {

  private static final String DBC = "DB123";
  private static final String HIDDEN_BY = "admin";
  private static final String REASON = "reason for hiding";
  private static final String GMC_ID = "1-ABCDE";
  private static final LocalDateTime BATCH_TIME = LocalDateTime.of(2026, 3, 5, 10, 0, 0);

  private final HiddenDiscrepancyMapper mapper =
      Mappers.getMapper(HiddenDiscrepancyMapper.class);

  @Test
  void shouldMapAllFieldsAndIgnoreIdWhenToEntityCalled() {
    // given
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of()) // doctors list is not mapped, so can be empty
        .build();

    // when
    HiddenDiscrepancy entity = mapper.toEntity(dto, GMC_ID, BATCH_TIME);

    // then
    assertThat(entity).isNotNull();

    // id ignored
    assertThat(entity.getId()).isNull();

    // mapped fields
    assertThat(entity.getGmcReferenceNumber()).isEqualTo(GMC_ID);
    assertThat(entity.getHiddenForDesignatedBodyCode()).isEqualTo(
        dto.getHiddenForDesignatedBodyCode());
    assertThat(entity.getHiddenBy()).isEqualTo(dto.getHiddenBy());
    assertThat(entity.getReason()).isEqualTo(dto.getReason());
    assertThat(entity.getHiddenDateTime()).isEqualTo(BATCH_TIME);
  }
}
