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

package uk.nhs.hee.tis.revalidation.connection.service;

import static java.util.function.Predicate.not;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

/*
 * Service class for managing hidden discrepancies related to doctors and designated bodies.
 */
@Slf4j
@Service
public class HiddenDiscrepancyService {

  private final HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
  private final HiddenDiscrepancyMapper hiddenDiscrepancyMapper;

  /**
   * Constructs a new HiddenDiscrepancyService with the specified repository and mapper.
   *
   * @param hiddenDiscrepancyRepository the repository for managing hidden discrepancies
   * @param hiddenDiscrepancyMapper     the mapper for converting between DTOs and entities
   */
  public HiddenDiscrepancyService(HiddenDiscrepancyRepository hiddenDiscrepancyRepository,
      HiddenDiscrepancyMapper hiddenDiscrepancyMapper) {
    this.hiddenDiscrepancyRepository = hiddenDiscrepancyRepository;
    this.hiddenDiscrepancyMapper = hiddenDiscrepancyMapper;
  }

  /**
   * Hides discrepancies for a list of doctors based on the provided DTO.
   *
   * @param dto the DTO containing information about which discrepancies to hide
   * @return a response DTO summarizing the results of the hide operation
   */
  public HideDiscrepancyResponseDto hideDiscrepancies(HideDiscrepancyDto dto) {
    final String hiddenForDbc = dto.getHiddenForDesignatedBodyCode();
    final List<String> requestedGmcIds = extractRequestedGmcIds(dto);

    if (requestedGmcIds.isEmpty()) {
      return HideDiscrepancyResponseDto.builder().hiddenForDesignatedBodyCode(hiddenForDbc).build();
    }

    final Set<String> alreadyHidden = findHiddenGmcIds(requestedGmcIds, hiddenForDbc);
    final List<String> newGmcIdsToHide = requestedGmcIds.stream()
        .filter(not(alreadyHidden::contains))
        .collect(Collectors.toList());
    final List<String> savedGmcIds = new ArrayList<>();

    if (!newGmcIdsToHide.isEmpty()) {
      final LocalDateTime batchTime = LocalDateTime.now();
      final List<HiddenDiscrepancy> newEntities = newGmcIdsToHide.stream()
          .map(gmcId -> hiddenDiscrepancyMapper.toEntity(dto, gmcId, batchTime))
          .collect(Collectors.toList());

      try {
        savedGmcIds.addAll(hiddenDiscrepancyRepository.saveAll(newEntities).stream()
            .map(HiddenDiscrepancy::getGmcId)
            .collect(Collectors.toList()));
      } catch (Exception e) {
        log.error("Error saving hidden discrepancies to the db", e);
      }
    }

    final List<String> failedToHide = newGmcIdsToHide.stream()
        .filter(not(savedGmcIds::contains))
        .collect(Collectors.toList());

    return new HideDiscrepancyResponseDto(hiddenForDbc, savedGmcIds, failedToHide,
        new ArrayList<>(alreadyHidden));
  }

  private List<String> extractRequestedGmcIds(HideDiscrepancyDto dto) {
    if (dto.getDoctors() == null) {
      return List.of();
    }
    return dto.getDoctors().stream()
        .map(DoctorInfoDto::getGmcId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  private Set<String> findHiddenGmcIds(List<String> gmcIds, String hiddenForDbc) {
    return hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(gmcIds, hiddenForDbc).stream()
        .map(HiddenDiscrepancy::getGmcId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
