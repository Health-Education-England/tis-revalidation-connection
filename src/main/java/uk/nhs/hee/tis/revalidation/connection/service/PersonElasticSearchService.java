package uk.nhs.hee.tis.revalidation.connection.service;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.connection.dto.PersonViewDTO;
import uk.nhs.hee.tis.revalidation.connection.entity.ColumnFilter;
import uk.nhs.hee.tis.revalidation.connection.entity.PersonView;
import uk.nhs.hee.tis.revalidation.connection.repository.PersonSearchRespository;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

@Service
public class PersonElasticSearchService {
  @Autowired
  PersonSearchRespository personElasticSearchRepository;

  public Page<PersonViewDTO> searchForPage(String searchQuery,
                                           List<ColumnFilter> columnFilters, Pageable pageable) {

    try {
      // iterate over the column filters, if they have multiple values per filter, place a should between then
      // for each column filter set, place a must between them
      BoolQueryBuilder mustBetweenDifferentColumnFilters = new BoolQueryBuilder();


      //apply free text search on the searchable columns
      BoolQueryBuilder shouldQuery = applyTextBasedSearchQuery(searchQuery);

      // add the free text query with a must to the column filters query
      BoolQueryBuilder fullQuery = mustBetweenDifferentColumnFilters.must(shouldQuery);

//    LOG.info("Query {}", fullQuery.toString());
      pageable = replaceSortByIdHack(pageable);


      Page<PersonView> result = personElasticSearchRepository.search(fullQuery, pageable);

      return new PageImpl<>(
          convertPersonViewToDTO(result.getContent()), pageable,
          result.getTotalElements());
    } catch (RuntimeException re) {
      //LOG.error("An exception occurred while attempting to do an ES search", re);
      throw re;
    }
  }

  private BoolQueryBuilder applyTextBasedSearchQuery(String searchQuery) {
    // this part is the free text part of the query, place a should between all of the searchable fields
    BoolQueryBuilder shouldQuery = new BoolQueryBuilder();
    if (StringUtils.isNotEmpty(searchQuery)) {
      searchQuery = StringUtils
          .remove(searchQuery, '"'); //remove any quotations that were added from the FE
      shouldQuery
          .should(new MatchQueryBuilder("publicHealthNumber", searchQuery))
          .should(new MatchQueryBuilder("fullName", searchQuery))
          .should(new WildcardQueryBuilder("surname", "*" + searchQuery + "*"))
          .should(new WildcardQueryBuilder("forenames", "*" + searchQuery + "*"))
          .should(new MatchQueryBuilder("gmcNumber", searchQuery))
          .should(new MatchQueryBuilder("gdcNumber", searchQuery))
          .should(new MatchQueryBuilder("role", searchQuery));

      if (StringUtils.isNumeric(searchQuery)) {
        shouldQuery = shouldQuery.should(new TermQueryBuilder("personId", searchQuery));
      }
    }

    //LOG.debug("Query is : {}", shouldQuery);
    return shouldQuery;
  }

  private List<PersonViewDTO> convertPersonViewToDTO(List<PersonView> content
                                                     /*ProgrammeMembershipStatus programmeMembershipStatus*/) {
/*    if (programmeMembershipStatus == null) {
      //programmeMembershipStatus = ProgrammeMembershipStatus.CURRENT;
    }*/
    //final ProgrammeMembershipStatus programmeMembershipStatusFilter = programmeMembershipStatus;
    return content.stream().map(pv -> {
      PersonViewDTO personViewDTO = new PersonViewDTO();
      personViewDTO.setId(pv.getPersonId());
      personViewDTO.setIntrepidId(pv.getIntrepidId());
      personViewDTO.setSurname(pv.getSurname());
      personViewDTO.setForenames(pv.getForenames());
      personViewDTO.setGmcNumber(pv.getGmcNumber());
      personViewDTO.setGdcNumber(pv.getGdcNumber());
      personViewDTO.setPublicHealthNumber(pv.getPublicHealthNumber());
      personViewDTO.setGradeId(pv.getGradeId());
      personViewDTO.setGradeAbbreviation(pv.getGradeAbbreviation());
      personViewDTO.setGradeName(pv.getGradeName());
      personViewDTO.setSiteId(pv.getSiteId());
      personViewDTO.setSiteCode(pv.getSiteCode());
      personViewDTO.setSiteName(pv.getSiteName());
      personViewDTO.setPlacementType(pv.getPlacementType());
      personViewDTO.setSpecialty(pv.getSpecialty());
      personViewDTO.setRole(pv.getRole());
      personViewDTO.setStatus(pv.getStatus());
      personViewDTO.setCurrentOwner(pv.getCurrentOwner());

      return personViewDTO;
    }).collect(Collectors.toList());
  }

  private Pageable replaceSortByIdHack(Pageable pageable) {
    //hack as we dont sort by id but rather personId - this can be removed once we remove the duplicate trainees from the
    //list view
    Sort sort = pageable.getSort();

    Iterator<Sort.Order> sortIterator = sort.iterator();
    List<Sort.Order> sortOrders = Lists.newArrayList();
    while (sortIterator.hasNext()) {
      Sort.Order order = sortIterator.next();
      if (!order.getProperty().equals("id")) {
        sortOrders.add(order);
      } else {
        if (order.isAscending()) {
          sortOrders.add(Sort.Order.asc("personId"));
        } else if (order.isDescending()) {
          sortOrders.add(Sort.Order.desc("personId"));
        } else {
          //yes i know, we actually send sort by id with no direction - doh
        }
      }
    }

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sortOrders));
  }
}
