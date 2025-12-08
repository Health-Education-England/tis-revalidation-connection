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

@Slf4j
@Repository
public class ConnectionLogCustomRepository {

  private static final String COLLECTION_NAME = "connectionLogs";
  private final MongoTemplate mongoTemplate;

  public ConnectionLogCustomRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

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

    int totalPages = (int) Math.ceil((double) total / pageSize);
    log.debug("Total records: {}, Total Pages: {}", total, totalPages);

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
