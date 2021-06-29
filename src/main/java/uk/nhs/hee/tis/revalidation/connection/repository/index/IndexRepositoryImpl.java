/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.connection.repository.index;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.repository.index.elasticsearch.ElasticSearchIndexRepository;


@Repository
public class IndexRepositoryImpl<T extends BaseConnectionView> implements IndexRepository<T> {
  ElasticSearchIndexRepository<T> elasticsearchIndexRepository;

  public IndexRepositoryImpl(
      ElasticSearchIndexRepository<T> elasticsearchIndexRepository
  ) {
    this.elasticsearchIndexRepository = elasticsearchIndexRepository;
  }

  @Override
  public Iterable<T> executeQuery(BoolQueryBuilder fullQuery) {
    return elasticsearchIndexRepository.search(fullQuery);
  }

  @Override
  public Page<T> executePagedQuery(BoolQueryBuilder fullQuery, Pageable pageable) {
    return elasticsearchIndexRepository.search(fullQuery, pageable);
  }

  public List<T> findViewByGmcReferenceNumberAndTcsPersonId(String gmcReferenceNumber,
                                                            Long tcsPersonId) {
    return elasticsearchIndexRepository
        .findViewByGmcReferenceNumberAndTcsPersonId(gmcReferenceNumber,tcsPersonId);
  }

  public List<T> findViewByGmcReferenceNumber(String gmcReferenceNumber) {
    return elasticsearchIndexRepository.findViewByGmcReferenceNumber(gmcReferenceNumber);
  }

  public List<T> findViewByTcsPersonId(Long tcsPersonId) {
    return elasticsearchIndexRepository.findViewByTcsPersonId(tcsPersonId);
  }

  @Override
  public void save(T viewToSave) {
    elasticsearchIndexRepository.save(viewToSave);
  }

  @Override
  public void deleteViewByGmcReferenceNumber(String gmcReferenceNumber) {
    elasticsearchIndexRepository.deleteViewByGmcReferenceNumber(gmcReferenceNumber);
  }

  @Override
  public void deleteViewByTcsPersonId(Long tcsPersonId) {
    elasticsearchIndexRepository.deleteViewByTcsPersonId(tcsPersonId);
  }
}
