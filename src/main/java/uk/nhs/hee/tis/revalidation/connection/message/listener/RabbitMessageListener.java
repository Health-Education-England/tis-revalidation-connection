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

package uk.nhs.hee.tis.revalidation.connection.message.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.GmcDoctor;
import uk.nhs.hee.tis.revalidation.connection.message.receiver.ConnectionMessageReceiver;
import uk.nhs.hee.tis.revalidation.connection.message.receiver.GmcDoctorMessageReceiver;
import uk.nhs.hee.tis.revalidation.connection.message.receiver.SyncMessageReceiver;


@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  ConnectionMessageReceiver connectionMessageReceiver;
  @Autowired
  GmcDoctorMessageReceiver gmcDoctorMessageReceiver;
  @Autowired
  SyncMessageReceiver syncMessageReceiver;

  /**
   * handle rabbit message.
   *
   * @param connectionInfo connection information of the trainee
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.update}")
  public void receiveMessageUpdate(final ConnectionInfoDto connectionInfo) {
    connectionMessageReceiver.handleMessage(connectionInfo);
  }

  /**
   * get trainee from Master index then update connection indexes.
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.getmaster}",
      ackMode = "NONE")
  public void receiveMessageGetMaster(final String getMaster) {
    syncMessageReceiver.handleMessage(getMaster);
  }

  /**
   * handle message from gmc client.
   *
   * @param doctor gmc doctor information
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connection.gmcupdate}")
  public void receiveMessageGmcDoctor(final GmcDoctor doctor) {
    gmcDoctorMessageReceiver.handleMessage(doctor);
  }
}
