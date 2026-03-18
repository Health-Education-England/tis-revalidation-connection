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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.connection.dto.HideDiscrepancyResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.connection.mapper.HiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.connection.repository.HiddenDiscrepancyRepository;

@ExtendWith(MockitoExtension.class)
class HiddenDiscrepancyServiceTest {

  private static final String DBC = "1-ABCDE";
  private static final String HIDDEN_BY = "admin";
  private static final String REASON = "reason";

  @Captor
  ArgumentCaptor<List<HiddenDiscrepancy>> saveCaptor;
  @Captor
  ArgumentCaptor<List<String>> gmcIdsCaptor;
  @Mock
  private HiddenDiscrepancyRepository hiddenDiscrepancyRepository;
  @Mock
  private HiddenDiscrepancyMapper hiddenDiscrepancyMapper;
  @InjectMocks
  private HiddenDiscrepancyService service;

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
        .doctors(List.of(doc("GMC1"), doc("GMC2")))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of(entity("GMC1"), entity("GMC2"))) // alreadyHidden
        .thenReturn(List.of(entity("GMC1"), entity("GMC2"))); // nowHidden

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 2, 0, 0);
    assertResponseLists(response, List.of(), List.of(), List.of("GMC1", "GMC2"));

    verify(hiddenDiscrepancyRepository, never()).saveAll(anyList());
    verifyNoInteractions(hiddenDiscrepancyMapper);
  }

  @Test
  void shouldSaveOnlyNewGmcIdsAndReturnSuccessfulAndExistingListsWhenSomeAreNew() {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc("GMC1"), doc("GMC2"), doc("GMC3")))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of(entity("GMC1")));
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity("GMC2"), entity("GMC3"))); // nowHidden

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 1, 2, 0);
    assertResponseLists(response, List.of("GMC2", "GMC3"), List.of(), List.of("GMC1"));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    List<HiddenDiscrepancy> saved = saveCaptor.getValue();
    assertSavedEntities(saved, Set.of("GMC2", "GMC3"), dto);

    // mapper should be called only for new ids
    verify(hiddenDiscrepancyMapper, never()).toEntity(eq(dto), eq("GMC1"),
        any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper).toEntity(eq(dto), eq("GMC2"), any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper).toEntity(eq(dto), eq("GMC3"), any(LocalDateTime.class));
  }

  @Test
  void shouldContinueAndReturnListsComputedFromDbWhenSaveAllThrowsException() {
    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of());
    when(hiddenDiscrepancyRepository.saveAll(saveCaptor.capture()))
        .thenReturn(List.of(entity("GMC1"))); // nowHidden only has GMC1

    mockMapperToReturnRealEntity();
    doThrow(new RuntimeException("db down")).when(hiddenDiscrepancyRepository).saveAll(anyList());

    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc("GMC1"), doc("GMC2")))
        .build();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 1, 1);
    assertResponseLists(response, List.of("GMC1"), List.of("GMC2"), List.of());

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    assertSavedEntities(saveCaptor.getValue(), Set.of("GMC1", "GMC2"), dto);
  }

  @Test
  void shouldReturnFailedListWhenDbDoesNotReturnAllAsHiddenAfterSaveAttempt() {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc("GMC1"), doc("GMC2"), doc("GMC3")))
        .build();

    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of());
    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity("GMC1"), entity("GMC2"))); // nowHidden missing GMC3

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 2, 1);
    assertResponseLists(response, List.of("GMC1", "GMC2"), List.of("GMC3"), List.of()
    );
  }

  @Test
  void shouldDedupRequestedGmcIdsAndReturnListsWithoutDuplicatesWhenInputContainsDuplicates() {
    HideDiscrepancyDto dto = HideDiscrepancyDto.builder()
        .hiddenForDesignatedBodyCode(DBC)
        .hiddenBy(HIDDEN_BY)
        .reason(REASON)
        .doctors(List.of(doc("GMC1"), doc("GMC1"), doc("GMC2")))
        .build();

    // capture the gmcIds list passed into repository (to ensure distinct() is applied)
    when(hiddenDiscrepancyRepository
        .findByGmcIdInAndHiddenForDesignatedBodyCode(anyList(), eq(DBC)))
        .thenReturn(List.of());

    when(hiddenDiscrepancyRepository.saveAll(anyList()))
        .thenReturn(List.of(entity("GMC1"), entity("GMC2")));

    mockMapperToReturnRealEntity();

    HideDiscrepancyResponseDto response = service.hideDiscrepancies(dto);

    assertResponseListCounts(response, 0, 2, 0);
    assertResponseLists(response, List.of("GMC1", "GMC2"), List.of(), List.of());

    verify(hiddenDiscrepancyRepository)
        .findByGmcIdInAndHiddenForDesignatedBodyCode(gmcIdsCaptor.capture(), eq(DBC));
    List<String> requestedGmcsAtFirstQuery = gmcIdsCaptor.getValue();
    assertListContainsExactlyIgnoringOrder(requestedGmcsAtFirstQuery, List.of("GMC1", "GMC2"));

    verify(hiddenDiscrepancyRepository).saveAll(saveCaptor.capture());
    assertSavedEntities(saveCaptor.getValue(), Set.of("GMC1", "GMC2"), dto);

    verify(hiddenDiscrepancyMapper, times(1)).toEntity(eq(dto), eq("GMC1"),
        any(LocalDateTime.class));
    verify(hiddenDiscrepancyMapper, times(1)).toEntity(eq(dto), eq("GMC2"),
        any(LocalDateTime.class));
  }

  // -------------------- Helpers --------------------

  private static DoctorInfoDto doc(String gmc) {
    DoctorInfoDto d = mock(DoctorInfoDto.class);
    when(d.getGmcId()).thenReturn(gmc);
    return d;
  }

  /**
   * Simple entity builder for repository return values (only gmcId is relevant for service logic)
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
   * Assert the counts in the response and that the lists are non-null (but not their contents)
   *
   * @param response
   * @param expectedExisting
   * @param expectedSuccessful
   * @param expectedFailed
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
   * Assert the contents of the three lists in the response (ignoring order)
   *
   * @param response
   * @param expectedSuccessful
   * @param expectedFailed
   * @param expectedExisting
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
   * batchTime
   *
   * @param saved
   * @param expectedGmcs
   * @param dto
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
