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

import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;

/**
 * Mapper interface for converting HideDiscrepancyDto objects to HiddenDiscrepancy entities.
 * This mapper uses MapStruct to automatically generate the implementation code for the mapping.
 */
@Mapper(componentModel = "spring")
public interface HiddenDiscrepancyMapper {

  /**
   * Maps a HideDiscrepancyDto to a HiddenDiscrepancy entity.
   *
   * @param dto the HideDiscrepancyDto containing the data to be mapped
   * @param gmcId the GMC ID of the doctor for whom the discrepancy is being hidden
   * @param batchTime the timestamp when the hiding action is performed
   * @return a HiddenDiscrepancy entity populated with data from the DTO and additional parameters
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hiddenDateTime", source = "batchTime")
  HiddenDiscrepancy toEntity(HideDiscrepancyDto dto, String gmcId, LocalDateTime batchTime);
}
