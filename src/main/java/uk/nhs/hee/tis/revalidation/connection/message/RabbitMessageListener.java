package uk.nhs.hee.tis.revalidation.connection.message;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class RabbitMessageListener {

  //@Autowired
  //private ESService;

  @RabbitListener(queues = "${app.rabbit.queue}")
  public void receivedMessage(final Object message) {
    //Read from queue
    //Find revalidationDoctorDto
    //Get ProgrammeMembershipType - Match if visitor  ||
    //Get ProgrammeMembershipEndDate - Match if expires
    //Push to Exception ES index
    //ALL OF THE ABOVE IN SERVICE
    //ESService.updateTrainee(gmcDoctor);
  }

}
