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
