package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;

/**
 * Mapper interface for converting HiddenDiscrepancy objects to HiddenDiscrepancyDto DTOs. This
 * mapper uses MapStruct to automatically generate the implementation code for the mapping.
 */
@Mapper(componentModel = "spring")
public interface HiddenDiscrepancyMapper {

  /**
   * Converts a HiddenDiscrepancy object to a HiddenDiscrepancyDto DTO.
   *
   * @param hiddenDiscrepancy the HiddenDiscrepancy object to convert
   * @return the corresponding HiddenDiscrepancyDto DTO
   */
  HiddenDiscrepancyDto toHiddenDiscrepancyDto(HiddenDiscrepancy hiddenDiscrepancy);

  /**
   * Converts a List of HiddenDiscrepancy objects to a List of HiddenDiscrepancyDto DTOs.
   *
   * @param hiddenDiscrepancies the HiddenDiscrepancies List to convert
   * @return the corresponding List of HiddenDiscrepancyDto DTOs
   */
  List<HiddenDiscrepancyDto> toHiddenDiscrepancyDtoList(List<HiddenDiscrepancy> hiddenDiscrepancies);
}
