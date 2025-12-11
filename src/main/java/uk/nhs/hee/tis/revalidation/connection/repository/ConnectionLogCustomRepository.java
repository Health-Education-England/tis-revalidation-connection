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

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ReplaceRootOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionLog;

/**
 * Custom repository for ConnectionLog with aggregation queries.
 */
@Slf4j
@Repository
public class ConnectionLogCustomRepository {

  protected static final String COLLECTION_NAME = "connectionLogs";
  private final MongoTemplate mongoTemplate;

  /**
   * Constructor for ConnectionLogCustomRepository.
   *
   * @param mongoTemplate the MongoTemplate to interact with MongoDB
   */
  public ConnectionLogCustomRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Retrieves the latest connection logs for each unique gmcId with pagination.
   * Only logs with responseCode "0" or missing responseCode are considered.
   * The pipeline includes filtering, sorting, grouping, and pagination stages as below:
   * [
   *   { "$match": { "$or": [{ "responseCode": "0" }, { "responseCode": { "$exists": false } }] } },
   *   { "$sort": { "gmcId": 1, "requestTime": -1 } },
   *   { "$group": { "_id": "$gmcId",
   * 				"latestRecord": { "$first": "$$ROOT" } } },
   *   { "$replaceRoot": { "newRoot": "$latestRecord" } },
   *   { "$skip": page * pageSize },
   *   { "$limit": pageSize }
   * ]
   *
   * @param page     the page number (0-based)
   * @param pageSize the number of records per page
   * @return a Page of ConnectionLog containing the latest logs
   */
  public Page<ConnectionLog> getLatestLogsWithPaging(int page, int pageSize) {
    // $match stage to filter logs with responseCode "0" or missing responseCode
    MatchOperation match = Aggregation.match(
        new Criteria().orOperator(
            Criteria.where("responseCode").is("0"),
            Criteria.where("responseCode").exists(false)
        )
    );

    // $sort stage to sort by gmcId ascending and requestTime descending
    SortOperation sort = Aggregation.sort(
        Sort.by(Sort.Order.asc("gmcId"), Sort.Order.desc("requestTime"))
    );

    // $group stage to group by gmcId and get the first document (latest log)
    GroupOperation group = Aggregation.group("gmcId")
        .first(Aggregation.ROOT).as("latestLog");

    // $replaceRoot stage to replace the root with the latestLog document
    ReplaceRootOperation replaceRoot = Aggregation.replaceRoot("latestLog");

    // $skip and $limit stages for pagination
    SkipOperation skip = Aggregation.skip((long) page * pageSize);
    LimitOperation limit = Aggregation.limit(pageSize);

    // Get the total count of records after grouping
    GroupOperation countGroup = Aggregation.group("gmcId");
    Aggregation countAggregation = Aggregation.newAggregation(match, countGroup,
        Aggregation.count().as("total"));

    var resultMap = mongoTemplate.aggregate(countAggregation, COLLECTION_NAME, Map.class)
        .getUniqueMappedResult();
    int total = resultMap == null ? 0 : (int) resultMap.get("total");

    log.debug("Total records: {}, Total Pages: {}", total,
        (int) Math.ceil((double) total / pageSize));

    // Final aggregation pipeline
    Aggregation aggregation = Aggregation.newAggregation(
        match, sort, group, replaceRoot, skip, limit
    );

    List<ConnectionLog> logs = mongoTemplate.aggregate(aggregation, COLLECTION_NAME,
            ConnectionLog.class)
        .getMappedResults();

    return new PageImpl<>(logs, PageRequest.of(page, pageSize), total);
  }
}
