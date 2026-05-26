package uk.nhs.hee.tis.revalidation.connection.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  private final String cronExpression;

  /**
   * Constructor for the HiddenDiscrepanciesExpiryCronService.
   *
   * @param hiddenDiscrepancyService the service to manage hidden discrepancies
   * @param cronExpression the cron expression for scheduling the job
   */
  @Autowired
  public HiddenDiscrepanciesExpiryCronService(
      HiddenDiscrepancyService hiddenDiscrepancyService,
      @Value(SPEL_CRON) String cronExpression) {
    this.hiddenDiscrepancyService = hiddenDiscrepancyService;
    this.cronExpression = cronExpression;
  }

  /**
   * Scheduled method to remove expired hidden discrepancies from the database. This method is
   * executed according to the cron expression defined in the application properties. It uses
   * ShedLock to ensure that only one instance of this job runs at a time.
   */
  @Scheduled(cron = SPEL_CRON)
  @SchedulerLock(
      name = LOCK_NAME,
      lockAtLeastFor = SPEL_CRONLOCK,
      lockAtMostFor = SPEL_CRONLOCK)
  public void removeExpiredHiddenDiscrepancies() {
    log.info("Cron job started to remove expired hidden discrepancies.");
    long start = System.currentTimeMillis();
    try {
      hiddenDiscrepancyService.removeExpiredHiddenDiscrepancies();
    } catch (Exception e) {
      log.error("Error occurred while removing expired hidden discrepancies.", e);
    } finally {
      long diff = System.currentTimeMillis() - start;
      log.info("Expired hidden discrepancies daily removal : EXIT took[{}]ms", diff);
    }
  }
}
