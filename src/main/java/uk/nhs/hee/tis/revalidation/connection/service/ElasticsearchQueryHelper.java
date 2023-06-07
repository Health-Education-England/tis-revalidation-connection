package uk.nhs.hee.tis.revalidation.connection.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class ElasticsearchQueryHelper {

  public static String formatDesignatedBodyCodesForElasticsearchQuery(
      List<String> designatedBodyCodes) {
    List<String> escapedCodes = new ArrayList<>();
    designatedBodyCodes.forEach(code ->
        escapedCodes.add(code.toLowerCase().replace("1-", ""))
    );
    return String.join(" ", escapedCodes);
  }
}
