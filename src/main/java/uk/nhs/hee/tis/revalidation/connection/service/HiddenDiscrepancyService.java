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
import java.util.List;
import java.util.ArrayList;
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

    var adminDbcs = dto.getAdminDesignatedBodyCodes();

    for (DoctorInfoDto doctor : dto.getDoctors()) {
      var item = new HideDiscrepancyResponseDto.HideDiscrepancyResponseItem();
      String gmcId = doctor.getGmcId();
      item.setGmcId(gmcId);

      // collect the doctor's designated body codes (may be null)
      var doctorDbcs = Stream.of(doctor.getCurrentDesignatedBodyCode(),
              doctor.getProgrammeOwnerDesignatedBodyCode())
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      // intersection with admin codes
      var toHide = adminDbcs.stream()
          .filter(doctorDbcs::contains)
          .distinct()
          .collect(Collectors.toList());

      if (toHide.isEmpty()) {
        // nothing to hide for this doctor
        response.getResults().add(item);
        continue;
      }

      for (String dbc : toHide) {
        try {
          // check existence
          var existing = hiddenDiscrepancyRepository
              .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(List.of(gmcId), List.of(dbc));
          if (existing != null && !existing.isEmpty()) {
            mergeExisting(item, dbc);
            continue;
          }

          var entity = hiddenDiscrepancyMapper.toEntity(dto, gmcId, LocalDateTime.now(), dbc);
          entity.setHiddenBy(dto.getHiddenBy());
          entity.setReason(dto.getReason());
          hiddenDiscrepancyRepository.save(entity);
          mergeSuccessful(item, dbc);
        } catch (Exception ex) {
          log.error("Failed to hide discrepancy for gmcId {} and dbc {}: {}", gmcId, dbc,
              ex.getMessage());
          mergeFailed(item, dbc);
        }
      }

      response.getResults().add(item);
    }

    return response;
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
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(gmcIds,
            List.of(hiddenForDbc)).stream()
        .map(HiddenDiscrepancy::getGmcId)
        .collect(Collectors.toSet());
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
