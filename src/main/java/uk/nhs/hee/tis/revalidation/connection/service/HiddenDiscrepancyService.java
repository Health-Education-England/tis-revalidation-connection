/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

@Slf4j
@Service
public class HiddenDiscrepancyService {

  private final HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
  private final HiddenDiscrepancyMapper hiddenDiscrepancyMapper;

  public HiddenDiscrepancyService(HiddenDiscrepancyRepository hiddenDiscrepancyRepository,
      HiddenDiscrepancyMapper hiddenDiscrepancyMapper) {
    this.hiddenDiscrepancyRepository = hiddenDiscrepancyRepository;
    this.hiddenDiscrepancyMapper = hiddenDiscrepancyMapper;
  }

  public void hideDiscrepancies(HideDiscrepancyDto hideDiscrepancyDto) {
    String dbc = hideDiscrepancyDto.getHiddenForDesignatedBodyCode();

    // Preprocess the GMC IDs: remove null/empty, trim, and remove duplicates
    List<String> normalizedGmcIds = hideDiscrepancyDto.getDoctorGmcIds().stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .collect(Collectors.toList());

    if (normalizedGmcIds.isEmpty()) {
      return;
    }
    // Filter out GMC IDs that are already hidden for the given DBC
    Set<String> existingGmcReferenceNumbersForDbc = hiddenDiscrepancyRepository
        .findByGmcReferenceNumberInAndHiddenForDesignatedBodyCode(normalizedGmcIds, dbc).stream()
        .map(HiddenDiscrepancy::getGmcReferenceNumber).collect(
            Collectors.toSet());

    List<String> newGmcIdsToHide = normalizedGmcIds.stream()
        .filter(gmcId -> !existingGmcReferenceNumbersForDbc.contains(gmcId))
        .collect(Collectors.toList());

    if (newGmcIdsToHide.isEmpty()) {
      return;
    }

    // Create new HiddenDiscrepancy entities and save them to the repository
    LocalDateTime now = LocalDateTime.now();

    List<HiddenDiscrepancy> newEntities = newGmcIdsToHide.stream()
        .map(gmcId -> hiddenDiscrepancyMapper.toEntity(hideDiscrepancyDto, gmcId, now))
        .collect(Collectors.toList());

    hiddenDiscrepancyRepository.saveAll(newEntities);
  }
}
