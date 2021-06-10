package uk.nhs.hee.tis.revalidation.connection.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Jacksonized
@Data
public class BaseConnectionView {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private String id;
  private Long tcsPersonId;
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  //@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate submissionDate;
  private String programmeName;
  private String membershipType;
  private String designatedBody;
  private String tcsDesignatedBody;
  private String programmeOwner;
  private String connectionStatus;
  //@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate membershipStartDate;
  //@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate membershipEndDate;
}
