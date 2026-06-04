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

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Cron service to remove expired hidden discrepancies from the database.
 */
@Slf4j
@Service
public class HiddenDiscrepanciesExpiryCronService {

  public static final String LOCK_NAME = "hiddenDiscrepanciesExpiryCronLock";

  public static final String PREFIX = "${app.reval.cron.hiddendiscrepancy.expiry.";

  public static final String SPEL_CRON = PREFIX + "expression}";
  public static final String SPEL_CRONLOCK = PREFIX + "lock}";

  private final HiddenDiscrepancyService hiddenDiscrepancyService;

  /**
   * Constructor for the HiddenDiscrepanciesExpiryCronService.
   *
   * @param hiddenDiscrepancyService the service to manage hidden discrepancies
   */
  @Autowired
  public HiddenDiscrepanciesExpiryCronService(
      HiddenDiscrepancyService hiddenDiscrepancyService) {
    this.hiddenDiscrepancyService = hiddenDiscrepancyService;
  }

  /**
   * Scheduled method to remove expired hidden discrepancies from the database. This method is
   * executed according to the cron expression defined in the application properties. It uses
   * ShedLock to ensure that only one instance of this job runs at a time.
   */
  @Scheduled(cron = SPEL_CRON)
  @SchedulerLock(
      name = LOCK_NAME,
      lockAtMostFor = SPEL_CRONLOCK)
  public void removeExpiredHiddenDiscrepancies() {
    log.info("Cron job started to remove expired hidden discrepancies.");
    long start = System.currentTimeMillis();
    try {
      hiddenDiscrepancyService.removeExpiredHiddenDiscrepancies();
    } finally {
      long diff = System.currentTimeMillis() - start;
      log.info("Expired hidden discrepancies daily removal : EXIT took[{}]ms", diff);
    }
  }
}
