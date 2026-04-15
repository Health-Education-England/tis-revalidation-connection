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

import java.util.List;
import org.mapstruct.Mapper;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.MasterDoctorView;

/**
 * Mapper interface for converting MasterDoctorView objects to HiddenDiscrepancyInfoDto DTOs. This
 * mapper uses MapStruct to automatically generate the implementation code for the mapping.
 */
@Mapper(componentModel = "spring")
public interface HiddenDiscrepancyInfoMapper {

  /**
   * Maps a MasterDoctorView object to a HiddenDiscrepancyInfoDto object.
   *
   * @param masterDoctorView the MasterDoctorView object to be mapped
   * @return the corresponding HiddenDiscrepancyInfoDto object
   */
  HiddenDiscrepancyInfoDto toHiddenDiscrepancyInfoDto(MasterDoctorView masterDoctorView);

  /**
   * Maps a List of MasterDoctorView objects to a List of HiddenDiscrepancyInfoDto objects.
   *
   * @param masterDoctorViews the List of MasterDoctorView objects to be mapped
   * @return the corresponding HiddenDiscrepancyInfoDto List object
   */
  List<HiddenDiscrepancyInfoDto> toHiddenDiscrepancyInfoDtoList(
      List<MasterDoctorView> masterDoctorViews);
}
