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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.mapper.HideDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.message.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

@ExtendWith(MockitoExtension.class)
class HiddenDiscrepancyServiceTest {

  private static final String ADMIN_DBC_1 = "1-ABCDE";
  private static final String ADMIN_DBC_2 = "1-EDCBA";
  private static final String HIDDEN_BY = "admin";
  private static final String REASON = "reason";
  private static final LocalDateTime HIDDEN_UNTIL = LocalDateTime.now().plusDays(30);
  private static final String EXCHANGE = "exchange";
  private static final String ES_SYNC_DATA_ROUTING_KEY = "esSyncDataRoutingKey";
  private static final String GMC_ID_1 = "GMC1";
  private static final String GMC_ID_2 = "GMC2";
  private static final String GMC_ID_3 = "GMC3";
  private HiddenDiscrepancy hd1;
  private HiddenDiscrepancy hd2;

  @Captor
  ArgumentCaptor<List<HiddenDiscrepancy>> saveCaptor;
  @Captor
  ArgumentCaptor<HiddenDiscrepancy> deleteCaptor;
  @Captor
  ArgumentCaptor<List<String>> gmcIdsCaptor;
  @Captor
  ArgumentCaptor<IndexSyncMessage<List<HiddenDiscrepancy>>> syncMessageCaptor;
  @Mock
  private HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
  @Mock
  private HideDiscrepancyMapper hideDiscrepancyMapper;
  @Mock
  private HiddenDiscrepancyMapper hiddenDiscrepancyMapper;
  @Mock
  private RabbitTemplate rabbitTemplate;
  @InjectMocks
  private HiddenDiscrepancyService service;

  @BeforeEach
  void setup() {
    setField(service, "exchange", EXCHANGE);
    setField(service, "esSyncDataRoutingKey", ES_SYNC_DATA_ROUTING_KEY);

    hd1 = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDateTime(HIDDEN_UNTIL)
        .build();
    hd2 = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_2)
        .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDateTime(HIDDEN_UNTIL)
        .build();
  }

  private static Stream<List<String>> invalidAdminDbcsSupplier() {
    return Stream.of(
        null,
        List.of()
    );
  }

  private static Stream<List<DoctorInfoDto>> invalidDoctorlistSupplier() {
    return Stream.of(
        null,
        List.of()
    );
  }

  private static Stream<List<DoctorInfoDto>> invalidDoctorGmcIdsSupplier() {
    return Stream.of(
        List.of(doc(null)),
        List.of(doc(null), doc(""))
    );
  }

  @ParameterizedTest
  @MethodSource("invalidDoctorlistSupplier")
  void shouldReturnEmptyResponseWhenDoctorsInvalid(List<DoctorInfoDto> doctors) {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1, ADMIN_DBC_2))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(doctors)
        .build();

    assertThrows(IllegalArgumentException.class, () -> service.hideDiscrepancies(dto));

    verifyNoInteractions(hiddenDiscrepancyRepository);
    verifyNoInteractions(hideDiscrepancyMapper);
  }

  @ParameterizedTest
  @MethodSource("invalidAdminDbcsSupplier")
  void shouldReturnEmptyResponseWhenAdminDbcInvalid(List<String> adminDbcs) {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(adminDbcs)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1)))
        .build();

    assertThrows(IllegalArgumentException.class, () -> service.hideDiscrepancies(dto));

    verifyNoInteractions(hiddenDiscrepancyRepository);
    verifyNoInteractions(hideDiscrepancyMapper);
  }

  @ParameterizedTest
  @MethodSource("invalidDoctorGmcIdsSupplier")
  void shouldReturnEmptyResponseWhenGmcIdInvalid(List<DoctorInfoDto> doctors) {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(doctors)
        .build();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(0, response.getResults().size());

    verifyNoInteractions(hiddenDiscrepancyRepository);
    verifyNoInteractions(hideDiscrepancyMapper);
  }

  @Test
  void shouldReturnEmptyResponseWhenDtoInvalid() {
    assertThrows(NullPointerException.class, () -> service.hideDiscrepancies(null));

    verifyNoInteractions(hiddenDiscrepancyRepository);
    verifyNoInteractions(hideDiscrepancyMapper);
  }

  @Test
  void shouldNotSaveAndShouldReturnExistingHiddenWhenAllRequestedAreAlreadyHidden() {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1, ADMIN_DBC_1, null), doc(GMC_ID_2, ADMIN_DBC_1, null)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(anyList(), anyList()))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2)));

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertNotNull(response);
    var resultMap2 = response.getResults().stream()
        .collect(Collectors.toMap(r -> r.getGmcId(), r -> r));

    var item1 = resultMap2.get(GMC_ID_1);
    assertNotNull(item1);
    assertListContainsExactlyIgnoringOrder(item1.getExistingDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(
        item1.getSuccessfulDbcCodes() == null ? List.of() : item1.getSuccessfulDbcCodes(),
        List.of());
    assertListContainsExactlyIgnoringOrder(
        item1.getFailedDbcCodes() == null ? List.of() : item1.getFailedDbcCodes(), List.of());

    var item2 = resultMap2.get(GMC_ID_2);
    assertNotNull(item2);
    assertListContainsExactlyIgnoringOrder(item2.getExistingDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(
        item2.getSuccessfulDbcCodes() == null ? List.of() : item2.getSuccessfulDbcCodes(),
        List.of());
    assertListContainsExactlyIgnoringOrder(
        item2.getFailedDbcCodes() == null ? List.of() : item2.getFailedDbcCodes(), List.of());

    verify(hiddenDiscrepancyRepository, never()).saveAll(anyList());
    verifyNoInteractions(hideDiscrepancyMapper);
  }

  @Test
  void shouldSaveOnlyNewGmcIdsAndReturnSuccessfulAndExistingListsWhenSomeAreNew() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .hiddenUntilDateTime(HIDDEN_UNTIL)
        .doctors(List.of(doc(GMC_ID_1, ADMIN_DBC_1, null), doc(GMC_ID_2, ADMIN_DBC_1, null),
            doc(GMC_ID_3, ADMIN_DBC_1, null)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(anyList(), anyList()))
        .thenReturn(List.of(entity(GMC_ID_1)));
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_2), entity(GMC_ID_3)));

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertNotNull(response);
    var resultMap3 = response.getResults().stream()
        .collect(Collectors.toMap(r -> r.getGmcId(), r -> r));

    // GMC1 existing
    var it1 = resultMap3.get(GMC_ID_1);
    assertNotNull(it1);
    assertListContainsExactlyIgnoringOrder(it1.getExistingDbcCodes(), List.of(ADMIN_DBC_1));

    // GMC2 and GMC3 successful
    var it2 = resultMap3.get(GMC_ID_2);
    var it3 = resultMap3.get(GMC_ID_3);
    assertNotNull(it2);
    assertNotNull(it3);
    assertListContainsExactlyIgnoringOrder(it2.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(it3.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    List<HiddenDiscrepancy> saved = saveCaptor.getValue();
    assertSavedEntities(saved, Set.of(GMC_ID_2, GMC_ID_3), dto);

    // mapper should be called only for new ids
    verify(hideDiscrepancyMapper, never()).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class), anyString());
    verify(hideDiscrepancyMapper).toEntity(eq(dto), eq(GMC_ID_2), any(LocalDateTime.class),
        anyString());
    verify(hideDiscrepancyMapper).toEntity(eq(dto), eq(GMC_ID_3), any(LocalDateTime.class),
        anyString());
  }

  @Test
  void shouldReturnFailedListWhenDbDoesNotReturnAllAsHiddenAfterSaveAttempt() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1, ADMIN_DBC_1, null), doc(GMC_ID_2, ADMIN_DBC_1, null),
            doc(GMC_ID_3, ADMIN_DBC_1, null)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(anyList(), anyList()))
        .thenReturn(List.of());
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2)));

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertNotNull(response);
    var resultMap4 = response.getResults().stream()
        .collect(Collectors.toMap(r -> r.getGmcId(), r -> r));

    var a1 = resultMap4.get(GMC_ID_1);
    var a2 = resultMap4.get(GMC_ID_2);
    var a3 = resultMap4.get(GMC_ID_3);
    assertNotNull(a1);
    assertNotNull(a2);
    assertNotNull(a3);
    assertListContainsExactlyIgnoringOrder(a1.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(a2.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(a3.getFailedDbcCodes(), List.of(ADMIN_DBC_1));
  }

  @Test
  void shouldRespondWithoutDuplicatesWhenInputContainsDuplicates() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1, ADMIN_DBC_1, null), doc(GMC_ID_1, ADMIN_DBC_1, null),
            doc(GMC_ID_2, ADMIN_DBC_1, null)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(anyList(), anyList()))
        .thenReturn(List.of());

    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2)));

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertNotNull(response);
    var resultMap5 = response.getResults().stream()
        .collect(Collectors.toMap(r -> r.getGmcId(), r -> r));
    var b1 = resultMap5.get(GMC_ID_1);
    var b2 = resultMap5.get(GMC_ID_2);
    assertNotNull(b1);
    assertNotNull(b2);
    assertListContainsExactlyIgnoringOrder(b1.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(b2.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(
        b1.getFailedDbcCodes() == null ? List.of() : b1.getFailedDbcCodes(), List.of());
    assertListContainsExactlyIgnoringOrder(
        b2.getFailedDbcCodes() == null ? List.of() : b2.getFailedDbcCodes(), List.of());

    verify(hiddenDiscrepancyRepository)
        .findByGmcIdInAndHiddenForDesignatedBodyCodeIn(gmcIdsCaptor.capture(),
            eq(List.of(ADMIN_DBC_1)));
    List<String> requestedGmcsAtFirstQuery = gmcIdsCaptor.getValue();
    assertListContainsExactlyIgnoringOrder(requestedGmcsAtFirstQuery, List.of(GMC_ID_1, GMC_ID_2));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    assertSavedEntities(saveCaptor.getValue(), Set.of(GMC_ID_1, GMC_ID_2), dto);

    verify(hideDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class), anyString());
    verify(hideDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_2),
        any(LocalDateTime.class), anyString());
  }

  @Test
  void shouldHideDiscrepanciesWhenOneDoctorCoversBothDbcsAndAnotherCoversOne() {
    // admin has two dbcs
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .adminDesignatedBodyCodes(List.of(ADMIN_DBC_1, ADMIN_DBC_2))
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        // doctor1 covers both dbcs, doctor2 covers only first dbc
        .doctors(List.of(doc(GMC_ID_1, ADMIN_DBC_1, ADMIN_DBC_2), doc(GMC_ID_2, ADMIN_DBC_1, null)))
        .build();

    // none exist beforehand
    when(hiddenDiscrepancyRepository.findByGmcIdInAndHiddenForDesignatedBodyCodeIn(anyList(),
        anyList()))
        .thenReturn(List.of());

    // mapper should produce real entities for each toSave
    mockMapperToReturnRealEntity();

    // make saveAll return the passed list (simulate successful save)
    when(hiddenDiscrepancyRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    // expect two result items (one per doctor)
    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(2, response.getResults().size());

    var resultMap = response.getResults().stream()
        .collect(Collectors.toMap(r -> r.getGmcId(), r -> r));

    var r1 = resultMap.get(GMC_ID_1);
    // doctor1 should have both successful dbcs
    assertNotNull(r1);
    assertListContainsExactlyIgnoringOrder(r1.getSuccessfulDbcCodes(),
        List.of(ADMIN_DBC_1, ADMIN_DBC_2));
    assertListContainsExactlyIgnoringOrder(
        r1.getFailedDbcCodes() == null ? List.of() : r1.getFailedDbcCodes(), List.of());
    assertListContainsExactlyIgnoringOrder(
        r1.getExistingDbcCodes() == null ? List.of() : r1.getExistingDbcCodes(), List.of());

    var r2 = resultMap.get(GMC_ID_2);
    // doctor2 should have only first dbc successful
    assertNotNull(r2);
    assertListContainsExactlyIgnoringOrder(r2.getSuccessfulDbcCodes(), List.of(ADMIN_DBC_1));
    assertListContainsExactlyIgnoringOrder(
        r2.getFailedDbcCodes() == null ? List.of() : r2.getFailedDbcCodes(), List.of());
    assertListContainsExactlyIgnoringOrder(
        r2.getExistingDbcCodes() == null ? List.of() : r2.getExistingDbcCodes(), List.of());

    // verify mapper called 3 times (2 for doctor1, 1 for doctor2)
    verify(hideDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class), eq(ADMIN_DBC_1));
    verify(hideDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class), eq(ADMIN_DBC_2));
    verify(hideDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_2),
        any(LocalDateTime.class), eq(ADMIN_DBC_1));
  }

  @Test
  void shouldSendHiddenDiscrepanciesForSync() {
    final HiddenDiscrepancy hiddenDiscrepancy = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .build();

    final Page<HiddenDiscrepancy> page = new PageImpl<>(List.of(hiddenDiscrepancy));

    when(hiddenDiscrepancyRepository.findAll(any(PageRequest.class))).thenReturn(page);

    service.sendHiddenDiscrepanciesForSync(1);

    verify(rabbitTemplate, times(2))
        .convertAndSend(eq(EXCHANGE), eq(ES_SYNC_DATA_ROUTING_KEY), syncMessageCaptor.capture());

    var result = syncMessageCaptor.getAllValues();
    IndexSyncMessage<List<HiddenDiscrepancy>> message1 = result.get(0);
    IndexSyncMessage<List<HiddenDiscrepancy>> message2 = result.get(1);

    assertThat(message1.getPayload()).containsExactly(hiddenDiscrepancy);
    assertThat(message1.getSyncEnd()).isFalse();

    assertThat(message2.getPayload()).isNull();
    assertThat(message2.getSyncEnd()).isTrue();
  }

  @Test
  void shouldPaginateHiddenDiscrepanciesForSync() {
    Page<HiddenDiscrepancy> page1 = new PageImpl<>(List.of(hd1), PageRequest.of(0, 1), 2);
    Page<HiddenDiscrepancy> page2 = new PageImpl<>(List.of(hd2), PageRequest.of(1, 1), 2);

    when(hiddenDiscrepancyRepository.findAll(any(PageRequest.class)))
        .thenReturn(page1)
        .thenReturn(page2);

    service.sendHiddenDiscrepanciesForSync(1);

    verify(rabbitTemplate, times(3))
        .convertAndSend(eq(EXCHANGE), eq(ES_SYNC_DATA_ROUTING_KEY), syncMessageCaptor.capture());

    var messages = syncMessageCaptor.getAllValues();
    assertThat(messages).hasSize(3);

    IndexSyncMessage<List<HiddenDiscrepancy>> msg1 = messages.get(0);
    assertThat(msg1.getPayload()).containsExactly(hd1);
    assertThat(msg1.getSyncEnd()).isFalse();

    IndexSyncMessage<List<HiddenDiscrepancy>> msg2 = messages.get(1);
    assertThat(msg2.getPayload()).containsExactly(hd2);
    assertThat(msg2.getSyncEnd()).isFalse();

    IndexSyncMessage<List<HiddenDiscrepancy>> msg3 = messages.get(2);
    assertThat(msg3.getPayload()).isNull();
    assertThat(msg3.getSyncEnd()).isTrue();
  }

  @Test
  void shouldSendSyncEndMessageOnlyIfNoDataAvailable() {

    Page<HiddenDiscrepancy> emptyPage =
        new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);

    when(hiddenDiscrepancyRepository.findAll(any(PageRequest.class)))
        .thenReturn(emptyPage);

    service.sendHiddenDiscrepanciesForSync(1);

    verify(rabbitTemplate)
        .convertAndSend(eq(EXCHANGE), eq(ES_SYNC_DATA_ROUTING_KEY), syncMessageCaptor.capture());

    var messages = syncMessageCaptor.getAllValues();
    assertThat(messages).hasSize(1);

    IndexSyncMessage<List<HiddenDiscrepancy>> msg = messages.get(0);
    assertThat(msg.getPayload()).isNull();
    assertThat(msg.getSyncEnd()).isTrue();
  }

  @Test
  void showDiscrepancyShouldRemoveAndReturnResponseWhenDiscrepancyPresent() {
    String hiddenDiscrepancyId = "507f1f77bcf86cd799439012";
    HiddenDiscrepancy entity = HiddenDiscrepancy.builder()
        .id(hiddenDiscrepancyId)
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
        .build();
    when(hiddenDiscrepancyRepository.findById(hiddenDiscrepancyId))
        .thenReturn(Optional.of(entity));

    service.showDiscrepancy(hiddenDiscrepancyId);

    verify(hiddenDiscrepancyRepository).delete(deleteCaptor.capture());
    var result = deleteCaptor.getValue();
    assertThat(result.getGmcId()).isEqualTo(GMC_ID_1);
    assertThat(result.getHiddenForDesignatedBodyCode()).isEqualTo(ADMIN_DBC_1);
    assertThat(result.getId()).isEqualTo(hiddenDiscrepancyId);
  }

  @Test
  void showDiscrepancyShouldThrowExceptionWhenDiscrepancyNotFound() {
    String discrepancyId = "507f1f77bcf86cd799439012";
    when(hiddenDiscrepancyRepository.findById(discrepancyId))
        .thenReturn(java.util.Optional.empty());

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.showDiscrepancy(discrepancyId)
    );
    assertThat(ex.getMessage()).contains(discrepancyId);
    verify(hiddenDiscrepancyRepository, never()).delete(any());
  }

  @Test
  void shouldReturnHiddenDiscrepancyDtoListForGmcId() {
    List<HiddenDiscrepancy> expected = List.of(hd1, hd2);
    when(hiddenDiscrepancyRepository.findByGmcId(GMC_ID_1)).thenReturn(expected);
    when(hiddenDiscrepancyMapper.toHiddenDiscrepancyDtoList(expected))
        .thenReturn(List.of(
            HiddenDiscrepancyDto.builder()
                .gmcId(GMC_ID_1)
                .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
                .hiddenBy(HIDDEN_BY)
                .reason(REASON)
                .build(),
            HiddenDiscrepancyDto.builder()
                .gmcId(GMC_ID_1)
                .hiddenForDesignatedBodyCode(ADMIN_DBC_1)
                .hiddenBy(HIDDEN_BY)
                .reason(REASON)
                .build()
        ));

    List<HiddenDiscrepancyDto> result = service.findByGmcId(GMC_ID_1);

    verify(hiddenDiscrepancyRepository).findByGmcId(GMC_ID_1);
    assertNotNull(result);
    assertEquals(2, result.size());
    var dto1 = result.get(0);
    assertEquals(GMC_ID_1, dto1.getGmcId());
    assertEquals(ADMIN_DBC_1, dto1.getHiddenForDesignatedBodyCode());
    assertEquals(HIDDEN_BY, dto1.getHiddenBy());
    assertEquals(REASON, dto1.getReason());
    var dto2 = result.get(1);
    assertEquals(GMC_ID_1, dto2.getGmcId());
    assertEquals(ADMIN_DBC_1, dto2.getHiddenForDesignatedBodyCode());
    assertEquals(HIDDEN_BY, dto2.getHiddenBy());
    assertEquals(REASON, dto2.getReason());
  }

  @Test
  void shouldReturnEmptyListWhenNoHiddenDiscrepanciesFoundForGmcId() {
    when(hiddenDiscrepancyRepository.findByGmcId(GMC_ID_1)).thenReturn(List.of());

    List<HiddenDiscrepancyDto> result = service.findByGmcId(GMC_ID_1);

    verify(hiddenDiscrepancyRepository).findByGmcId(GMC_ID_1);
    assertNotNull(result);
    assertThat(result).isEmpty();
  }

  // -------------------- Helpers --------------------

  private static DoctorInfoDto doc(String gmc) {
    DoctorInfoDto d = mock(DoctorInfoDto.class);
    lenient().when(d.getGmcId()).thenReturn(gmc);
    return d;
  }

  private static DoctorInfoDto doc(String gmc, String currentDbc, String programmeDbc) {
    DoctorInfoDto d = mock(DoctorInfoDto.class);
    when(d.getGmcId()).thenReturn(gmc);
    when(d.getCurrentDesignatedBodyCode()).thenReturn(currentDbc);
    when(d.getProgrammeOwnerDesignatedBodyCode()).thenReturn(programmeDbc);
    return d;
  }

  /**
   * Simple entity builder for repository return values (only gmcId is relevant for service logic).
   *
   * @param gmcId The GMC Number for the returned object
   * @return The HiddenDiscrepancy
   */
  private HiddenDiscrepancy entity(String gmcId) {
    return HiddenDiscrepancy.builder().gmcId(gmcId).hiddenForDesignatedBodyCode(ADMIN_DBC_1)
        .build();
  }

  // Mock mapper to return a real entity based on the input dto, gmcId and supplied dbc
  private void mockMapperToReturnRealEntity() {
    when(hideDiscrepancyMapper.toEntity(any(HideDiscrepancyDto.class), anyString(),
        any(LocalDateTime.class), anyString()))
        .thenAnswer(inv -> {
          HideDiscrepancyDto dto = inv.getArgument(0, HideDiscrepancyDto.class);
          String gmcId = inv.getArgument(1, String.class);
          LocalDateTime batchTime = inv.getArgument(2, LocalDateTime.class);
          String dbc = inv.getArgument(3, String.class);

          return HiddenDiscrepancy.builder()
              .gmcId(gmcId)
              .hiddenForDesignatedBodyCode(dbc)
              .hiddenBy(dto.getHiddenBy())
              .reason(dto.getReason())
              .hiddenDateTime(batchTime)
              .hiddenUntilDateTime(dto.getHiddenUntilDateTime())
              .build();
        });
  }

  private void assertListContainsExactlyIgnoringOrder(List<String> actual, List<String> expected) {
    assertNotNull(actual);
    assertNotNull(expected);

    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
  }

  /**
   * Assert that the saved entities have the expected GMCs, correct mapping from dto and consistent
   * batchTime.
   *
   * @param saved        the entities saved
   * @param expectedGmcs the expected GMC numbers to have been saved
   * @param dto          the dto to verify
   */
  private void assertSavedEntities(List<HiddenDiscrepancy> saved, Set<String> expectedGmcs,
      HideDiscrepancyDto dto) {
    assertNotNull(saved);
    assertEquals(expectedGmcs.size(), saved.size());

    Set<String> savedGmcs = saved.stream()
        .map(HiddenDiscrepancy::getGmcId)
        .collect(Collectors.toSet());
    assertEquals(expectedGmcs, savedGmcs);

    for (HiddenDiscrepancy hd : saved) {
      assertEquals(dto.getAdminDesignatedBodyCodes().get(0), hd.getHiddenForDesignatedBodyCode());
      assertEquals(dto.getHiddenBy(), hd.getHiddenBy());
      assertEquals(dto.getReason(), hd.getReason());
      assertEquals(dto.getHiddenUntilDateTime(), hd.getHiddenUntilDateTime());
      assertNotNull(hd.getHiddenDateTime());
    }

    Set<LocalDateTime> times = saved.stream()
        .map(HiddenDiscrepancy::getHiddenDateTime)
        .collect(Collectors.toSet());
    assertEquals(1, times.size());
  }
}
