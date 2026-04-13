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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.LocalDateTime;
import java.util.List;
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
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.message.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

@ExtendWith(MockitoExtension.class)
class HiddenDiscrepancyServiceTest {

  private static final String DBC = "1-ABCDE";
  private static final String HIDDEN_BY = "admin";
  private static final String REASON = "reason";
  private static final String EXCHANGE = "exchange";
  private static final String ES_SYNC_DATA_ROUTING_KEY = "esSyncDataRoutingKey";
  private static final String GMC_ID_1 = "GMC1";
  private static final String GMC_ID_2 = "GMC2";
  private static final String GMC_ID_3 = "GMC3";

  @Captor
  ArgumentCaptor<List<HiddenDiscrepancy>> saveCaptor;
  @Captor
  ArgumentCaptor<List<String>> gmcIdsCaptor;
  @Captor
  ArgumentCaptor<IndexSyncMessage<List<HiddenDiscrepancy>>> syncMessageCaptor;
  @Mock
  private HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
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
  }

  @ParameterizedTest
  @MethodSource("invalidDoctorDtosSupplier")
  void shouldReturnEmptyResponseWhenDoctorsInvalid(List<DoctorInfoDto> doctors) {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(doctors)
        .build();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 0, 0);
    assertResponseLists(response, List.of(), List.of(), List.of());

    verifyNoInteractions(hiddenDiscrepancyRepository);
    verifyNoInteractions(hiddenDiscrepancyMapper);
  }

  private static Stream<List<DoctorInfoDto>> invalidDoctorDtosSupplier() {
    return Stream.of(
        null,
        List.of(),
        List.of(doc(null)),
        List.of(doc(null), doc(null))
    );
  }


  @Test
  void shouldNotSaveAndShouldReturnExistingHiddenWhenAllRequestedAreAlreadyHidden() {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1), doc(GMC_ID_2)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2))) // alreadyHidden
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2))); // nowHidden

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 2, 0, 0);
    assertResponseLists(response, List.of(), List.of(), List.of(GMC_ID_1, GMC_ID_2));

    verify(hiddenDiscrepancyRepository, never()).saveAll(anyList());
    verifyNoInteractions(hiddenDiscrepancyMapper);
  }

  @Test
  void shouldSaveOnlyNewGmcIdsAndReturnSuccessfulAndExistingListsWhenSomeAreNew() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1), doc(GMC_ID_2), doc(GMC_ID_3)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of(entity(GMC_ID_1)));
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_2), entity(GMC_ID_3))); // nowHidden

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 1, 2, 0);
    assertResponseLists(response, List.of(GMC_ID_2, GMC_ID_3), List.of(), List.of(GMC_ID_1));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    List<HiddenDiscrepancy> saved = saveCaptor.getValue();
    assertSavedEntities(saved, Set.of(GMC_ID_2, GMC_ID_3), dto);

    // mapper should be called only for new ids
    verify(hiddenDiscrepancyMapper, never()).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper).toEntity(eq(dto), eq(GMC_ID_2), any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper).toEntity(eq(dto), eq(GMC_ID_3), any(LocalDateTime.class));
  }

  @Test
  void shouldReturnFailedListWhenDbDoesNotReturnAllAsHiddenAfterSaveAttempt() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1), doc(GMC_ID_2), doc(GMC_ID_3)))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of());
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2))); // nowHidden missing GMC3

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 2, 1);
    assertResponseLists(response, List.of(GMC_ID_1, GMC_ID_2), List.of(GMC_ID_3), List.of()
    );
  }

  @Test
  void shouldDedupRequestedGmcIdsAndReturnListsWithoutDuplicatesWhenInputContainsDuplicates() {
    final HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc(GMC_ID_1), doc(GMC_ID_1), doc(GMC_ID_2)))
        .build();

    // capture the gmcIds list passed into repository (to ensure distinct() is applied)
    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of());

    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity(GMC_ID_1), entity(GMC_ID_2)));

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 2, 0);
    assertResponseLists(response, List.of(GMC_ID_1, GMC_ID_2), List.of(), List.of());

    verify(hiddenDiscrepancyRepository)
        .findByGmcIdInAndHiddenForDesignatedBodyCode(gmcIdsCaptor.capture(), eq(DBC));
    List<String> requestedGmcsAtFirstQuery = gmcIdsCaptor.getValue();
    assertListContainsExactlyIgnoringOrder(requestedGmcsAtFirstQuery, List.of(GMC_ID_1, GMC_ID_2));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    assertSavedEntities(saveCaptor.getValue(), Set.of(GMC_ID_1, GMC_ID_2), dto);

    verify(hiddenDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_1),
        any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper, times(1)).toEntity(eq(dto), eq(GMC_ID_2),
        any(LocalDateTime.class));
  }

  @Test
  void shouldSendHiddenDiscrepanciesForSync() {
    final HiddenDiscrepancy hiddenDiscrepancy = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(DBC)
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
    HiddenDiscrepancy hd1 = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_1)
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .build();
    HiddenDiscrepancy hd2 = HiddenDiscrepancy.builder()
        .gmcId(GMC_ID_2)
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .build();

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

  // -------------------- Helpers --------------------

  private static DoctorInfoDto doc(String gmc) {
    DoctorInfoDto d = mock(DoctorInfoDto.class);
    when(d.getGmcId()).thenReturn(gmc);
    return d;
  }

  /**
   * Simple entity builder for repository return values (only gmcId is relevant for service logic).
   *
   * @param gmcId The GMC Number for the returned object
   * @return The HiddenDiscrepancy
   */
  private HiddenDiscrepancy entity(String gmcId) {
    return HiddenDiscrepancy.builder().gmcId(gmcId).build();
  }

  // Mock mapper to return a real entity based on the input dto and gmcId
  private void mockMapperToReturnRealEntity() {
    when(hiddenDiscrepancyMapper.toEntity(any(HideDiscrepancyDto.class), anyString(),
        any(LocalDateTime.class)))
        .thenAnswer(inv -> {
          HideDiscrepancyDto dto = inv.getArgument(0, HideDiscrepancyDto.class);
          String gmcId = inv.getArgument(1, String.class);
          LocalDateTime batchTime = inv.getArgument(2, LocalDateTime.class);

          return HiddenDiscrepancy.builder()
              .gmcId(gmcId)
              .hiddenForDesignatedBodyCode(dto.getHiddenForDesignatedBodyCode())
              .hiddenBy(dto.getHiddenBy())
              .reason(dto.getReason())
              .hiddenDateTime(batchTime)
              .build();
        });
  }

  /**
   * Assert the counts in the response and that the lists are non-null (but not their contents).
   *
   * @param response           the service response containing details of hidden discrepancies
   * @param expectedExisting   the expected number of existing discrepancies
   * @param expectedSuccessful the expected number of hide discrepancy updates to succeed
   * @param expectedFailed     the expected number of hide discrepancy updates to succeed
   */
  private void assertResponseListCounts(HideDiscrepancyResponseDto response, int expectedExisting,
      int expectedSuccessful, int expectedFailed) {
    assertNotNull(response);
    assertNotNull(response.getSuccessfulHiddenGmcIds());
    assertNotNull(response.getFailedToHideGmcIds());
    assertNotNull(response.getExistingHiddenGmcIds());
    assertEquals(HiddenDiscrepancyServiceTest.DBC, response.getHiddenForDesignatedBodyCode());

    assertEquals(expectedExisting, response.getExistingHiddenGmcIds().size());
    assertEquals(expectedSuccessful, response.getSuccessfulHiddenGmcIds().size());
    assertEquals(expectedFailed, response.getFailedToHideGmcIds().size());

  }

  /**
   * Assert the contents of the three lists in the response (ignoring order).
   *
   * @param response           the service response containing details of hidden discrepancies
   * @param expectedExisting   the expected number of existing discrepancies
   * @param expectedSuccessful the expected number of hide discrepancy updates to succeed
   * @param expectedFailed     the expected number of hide discrepancy updates to succeed
   */
  private void assertResponseLists(HideDiscrepancyResponseDto response,
      List<String> expectedSuccessful,
      List<String> expectedFailed,
      List<String> expectedExisting) {

    assertListContainsExactlyIgnoringOrder(response.getSuccessfulHiddenGmcIds(),
        expectedSuccessful);
    assertListContainsExactlyIgnoringOrder(response.getFailedToHideGmcIds(), expectedFailed);
    assertListContainsExactlyIgnoringOrder(response.getExistingHiddenGmcIds(), expectedExisting);
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
      assertEquals(dto.getHiddenForDesignatedBodyCode(), hd.getHiddenForDesignatedBodyCode());
      assertEquals(dto.getHiddenBy(), hd.getHiddenBy());
      assertEquals(dto.getReason(), hd.getReason());
      assertNotNull(hd.getHiddenDateTime());
    }

    Set<LocalDateTime> times = saved.stream()
        .map(HiddenDiscrepancy::getHiddenDateTime)
        .collect(Collectors.toSet());
    assertEquals(1, times.size());
  }
}
