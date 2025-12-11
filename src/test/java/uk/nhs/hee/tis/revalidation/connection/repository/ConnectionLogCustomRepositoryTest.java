/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.connection.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.connection.repository.ConnectionLogCustomRepository.COLLECTION_NAME;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;

@ExtendWith(MockitoExtension.class)
class ConnectionLogCustomRepositoryTest {

  private static final int PAGE_SIZE = 10;
  private static final int PAGE_NUMBER = 0;
  private static final int TOTAL_LOGS = 25;

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private AggregationResults<Map> countResults;

  @Mock
  private AggregationResults<ConnectionLog> logResults;

  @InjectMocks
  private ConnectionLogCustomRepository repository;

  @Test
  void shouldReturnPagedLogsCorrectly() {
    // given
    when(countResults.getUniqueMappedResult()).thenReturn(Map.of("total", TOTAL_LOGS));
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(COLLECTION_NAME), eq(Map.class)))
        .thenReturn(countResults);

    ConnectionLog log1 = new ConnectionLog();
    ConnectionLog log2 = new ConnectionLog();
    when(logResults.getMappedResults()).thenReturn(List.of(log1, log2));
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(COLLECTION_NAME),
        eq(ConnectionLog.class))).thenReturn(logResults);

    // when
    Page<ConnectionLog> pageResult = repository.getLatestLogsWithPaging(PAGE_NUMBER, PAGE_SIZE);

    // then
    assertEquals(25, pageResult.getTotalElements());
    assertEquals(2, pageResult.getContent().size());
    assertEquals(PAGE_SIZE, pageResult.getSize());

    verify(mongoTemplate).aggregate(any(Aggregation.class), eq(COLLECTION_NAME), eq(Map.class));
    verify(mongoTemplate).aggregate(any(Aggregation.class), eq(COLLECTION_NAME),
        eq(ConnectionLog.class));
  }

  @Test
  void shouldHandleNullCountResult() {
    // given
    // resultMap = null and total = 0
    when(countResults.getUniqueMappedResult()).thenReturn(null);
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(COLLECTION_NAME), eq(Map.class)))
        .thenReturn(countResults);

    when(logResults.getMappedResults()).thenReturn(List.of());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq(COLLECTION_NAME),
        eq(ConnectionLog.class))).thenReturn(logResults);
    // when
    Page<ConnectionLog> pageResult = repository.getLatestLogsWithPaging(PAGE_NUMBER, PAGE_SIZE);
    // then
    assertEquals(0, pageResult.getTotalElements());
    assertTrue(pageResult.getContent().isEmpty());

    verify(mongoTemplate).aggregate(any(Aggregation.class), eq(COLLECTION_NAME), eq(Map.class));
    verify(mongoTemplate).aggregate(any(Aggregation.class), eq(COLLECTION_NAME),
        eq(ConnectionLog.class));
  }
}
