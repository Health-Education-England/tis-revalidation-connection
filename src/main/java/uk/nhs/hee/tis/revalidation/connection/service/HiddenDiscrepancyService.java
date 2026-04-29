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

import static org.springframework.data.domain.PageRequest.of;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.message.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

/*
 * Service class for managing hidden discrepancies related to doctors and designated bodies.
 */
@Slf4j
@Service
public class HiddenDiscrepancyService {

  @Value("${app.rabbit.reval.exchange}")
  private String exchange;

  @Value("${app.rabbit.reval.routingKey.hiddendiscrepancy.essyncdata}")
  private String esSyncDataRoutingKey;

  private final RabbitTemplate rabbitTemplate;
  private final HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
  private final HiddenDiscrepancyMapper hiddenDiscrepancyMapper;

  /**
   * Constructs a new HiddenDiscrepancyService with the specified repository and mapper.
   *
   * @param hiddenDiscrepancyRepository the repository for managing hidden discrepancies
   * @param hiddenDiscrepancyMapper     the mapper for converting between DTOs and entities
   * @param rabbitTemplate              the template for sending rabbitmq messages
   */
  public HiddenDiscrepancyService(HiddenDiscrepancyRepository hiddenDiscrepancyRepository,
      HiddenDiscrepancyMapper hiddenDiscrepancyMapper, RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
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
    HideDiscrepancyResponseDto response = new HideDiscrepancyResponseDto();

    if (dto == null || dto.getDoctors() == null || dto.getDoctors().isEmpty()
        || dto.getAdminDesignatedBodyCodes() == null || dto.getAdminDesignatedBodyCodes()
        .isEmpty()) {
      return response;
    }

    List<String> adminDesignatedBodyCodes = dto.getAdminDesignatedBodyCodes();

    var responseItemsMap = prepareResponseItemsMap(dto.getDoctors());

    Map<String, List<String>> toHideByGmc = new HashMap<>();
    Set<String> allGmcIds = new HashSet<>();
    Set<String> allDbcs = new HashSet<>();
    prepareToHideData(dto.getDoctors(), adminDesignatedBodyCodes, toHideByGmc, allGmcIds, allDbcs);

    if (toHideByGmc.isEmpty()) {
      response.getResults().addAll(responseItemsMap.values());
      return response;
    }

    Set<String> existingPairs = queryExistingPairs(allGmcIds, allDbcs);

    List<HiddenDiscrepancy> toSave = buildEntitiesToSave(dto, toHideByGmc, existingPairs,
        responseItemsMap);

    saveEntitiesBatch(toSave, responseItemsMap);

    response.getResults().addAll(responseItemsMap.values());
    return response;
  }

  // Prepare a map of GMC ID to response item for easy lookup and result aggregation
  private Map<String, HideDiscrepancyResponseDto.HideDiscrepancyResponseItem> prepareResponseItemsMap(
      List<DoctorInfoDto> doctors) {
    var map = new HashMap<String, HideDiscrepancyResponseDto.HideDiscrepancyResponseItem>();
    for (DoctorInfoDto doctor : doctors) {
      String gmcId = doctor.getGmcId();
      if (gmcId != null) {
        var item = new HideDiscrepancyResponseDto.HideDiscrepancyResponseItem();
        item.setGmcId(gmcId);
        map.put(gmcId, item);
      }
    }
    return map;
  }

  private void prepareToHideData(List<DoctorInfoDto> doctors,
      List<String> adminDesignatedBodyCodes, Map<String, List<String>> toHideByGmc,
      Set<String> allGmcIds, Set<String> allDbcs) {

    for (DoctorInfoDto doctor : doctors) {
      String gmcId = doctor.getGmcId();
      if (gmcId != null) {
        Set<String> doctorDbcs = Stream.of(doctor.getCurrentDesignatedBodyCode(),
                doctor.getProgrammeOwnerDesignatedBodyCode())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<String> toHideList = adminDesignatedBodyCodes.stream()
            .filter(doctorDbcs::contains)
            .distinct()
            .collect(Collectors.toList());

        if (!toHideList.isEmpty()) {
          toHideByGmc.put(gmcId, toHideList);
          allGmcIds.add(gmcId);
          allDbcs.addAll(toHideList);
        }
      }
    }
  }

  // Query existing hidden discrepancies for the given GMC IDs and designated body codes to avoid duplicates
  private Set<String> queryExistingPairs(Set<String> allGmcIds, Set<String> allDbcs) {
    if (allGmcIds.isEmpty() || allDbcs.isEmpty()) {
      return Set.of();
    }
    var existing = hiddenDiscrepancyRepository.findByGmcIdInAndHiddenForDesignatedBodyCodeIn(
        new ArrayList<>(allGmcIds), new ArrayList<>(allDbcs));
    // Create a set of "GMCID#DBC" strings for easy lookup when building entities to save
    return existing.stream()
        .map(e -> buildKey(e.getGmcId(), e.getHiddenForDesignatedBodyCode()))
        .collect(Collectors.toSet());
  }

  private List<HiddenDiscrepancy> buildEntitiesToSave(HideDiscrepancyDto dto,
      Map<String, List<String>> toHideByGmc, Set<String> existingPairs,
      Map<String, HideDiscrepancyResponseDto.HideDiscrepancyResponseItem> responseItems) {
    List<HiddenDiscrepancy> toSave = new ArrayList<>();
    LocalDateTime saveTime = LocalDateTime.now();
    for (Map.Entry<String, List<String>> entry : toHideByGmc.entrySet()) {
      String gmcId = entry.getKey();
      var item = responseItems.get(gmcId);
      for (String dbc : entry.getValue()) {
        String key = buildKey(gmcId, dbc);
        if (existingPairs.contains(key)) {
          mergeExisting(item, dbc);
        } else {
          var entity = hiddenDiscrepancyMapper.toEntity(dto, gmcId, saveTime, dbc);
          toSave.add(entity);
        }
      }
    }
    return toSave;
  }

  private String buildKey(String gmcId, String dbc) {
    return gmcId + "#" + dbc;
  }

  private void saveEntitiesBatch(List<HiddenDiscrepancy> toSave,
      Map<String, HideDiscrepancyResponseDto.HideDiscrepancyResponseItem> responseItems) {
    if (toSave.isEmpty()) {
      return;
    }
    var saved = hiddenDiscrepancyRepository.saveAll(toSave);
    // build set of saved keys for comparison
    var savedKeys = saved.stream()
        .map(s -> buildKey(s.getGmcId(), s.getHiddenForDesignatedBodyCode()))
        .collect(Collectors.toSet());

    // mark all saved entries as successful
    for (HiddenDiscrepancy hd : saved) {
      mergeSuccessful(responseItems.get(hd.getGmcId()), hd.getHiddenForDesignatedBodyCode());
    }

    // any toSave element not present in savedKeys -> failed
    for (HiddenDiscrepancy hd : toSave) {
      String key = buildKey(hd.getGmcId(), hd.getHiddenForDesignatedBodyCode());
      if (!savedKeys.contains(key)) {
        mergeFailed(responseItems.get(hd.getGmcId()), hd.getHiddenForDesignatedBodyCode());
      }
    }
  }

  private void mergeSuccessful(HideDiscrepancyResponseDto.HideDiscrepancyResponseItem item,
      String dbc) {
    var list = item.getSuccessfulDbcCodes();
    if (list == null) {
      list = new ArrayList<>();
      item.setSuccessfulDbcCodes(list);
    }
    list.add(dbc);
  }

  private void mergeFailed(HideDiscrepancyResponseDto.HideDiscrepancyResponseItem item,
      String dbc) {
    var list = item.getFailedDbcCodes();
    if (list == null) {
      list = new ArrayList<>();
      item.setFailedDbcCodes(list);
    }
    list.add(dbc);
  }

  private void mergeExisting(HideDiscrepancyResponseDto.HideDiscrepancyResponseItem item,
      String dbc) {
    var list = item.getExistingDbcCodes();
    if (list == null) {
      list = new ArrayList<>();
      item.setExistingDbcCodes(list);
    }
    list.add(dbc);
  }

  /**
   * Show hidden discrepancy by removing the hidden discrepancy record with the specified id.
   *
   * @param discrepancyId the id of the hidden discrepancy to hide
   */
  public void showDiscrepancy(String discrepancyId) {
    hiddenDiscrepancyRepository.findById(discrepancyId).ifPresentOrElse(entity -> {
          hiddenDiscrepancyRepository.delete(entity);
          log.info("Successfully removed hidden discrepancy for GMC ID: {} and designated body: {}",
              entity.getGmcId(), entity.getHiddenForDesignatedBodyCode());
        }, () -> {
          throw new
              IllegalArgumentException("No hidden discrepancy found with id: " + discrepancyId);
        }
    );
  }

  /**
   * Send hidden discrepancies to rabbit for elasticsearch sync in pages.
   *
   * @param pageSize the size of each page
   */
  public void sendHiddenDiscrepanciesForSync(int pageSize) {
    int currentPage = 0;
    Page<HiddenDiscrepancy> hiddenDiscrepancies;
    do {
      hiddenDiscrepancies = hiddenDiscrepancyRepository.findAll(of(currentPage, pageSize));
      int totalPages = hiddenDiscrepancies.getTotalPages();
      if (totalPages == 0) {
        log.info("No hidden discrepancies to sync.");
        break;
      }
      log.info("Sending page {} of hidden discrepancies for sync. Total pages: {}",
          currentPage, totalPages);
      var payload = IndexSyncMessage.builder()
          .payload(hiddenDiscrepancies.toList())
          .syncEnd(false)
          .build();
      rabbitTemplate.convertAndSend(exchange, esSyncDataRoutingKey, payload);
      currentPage++;
    } while (currentPage < hiddenDiscrepancies.getTotalPages());
    var syncEndPayload = IndexSyncMessage.builder().syncEnd(true).build();
    rabbitTemplate.convertAndSend(exchange, esSyncDataRoutingKey, syncEndPayload);
  }
}
