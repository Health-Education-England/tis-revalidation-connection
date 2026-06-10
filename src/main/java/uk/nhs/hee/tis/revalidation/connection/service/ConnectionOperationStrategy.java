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

import uk.nhs.hee.tis.revalidation.connection.dto.DoctorInfoDto;
import uk.nhs.hee.tis.revalidation.connection.dto.GmcConnectionResponseDto;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestType;

/**
 * Strategy interface for different types of connection operations (add/remove).
 */
public interface ConnectionOperationStrategy {

  /**
   * Execute the GMC connection operation for a specific doctor.
   *
   * @param gmcClientService   the GMC client service to use for the operation
   * @param doctor             the doctor information
   * @param changeReason       the reason for the connection change
   * @param designatedBodyCode the designated body code from the request
   * @return the response from GMC
   */
  GmcConnectionResponseDto execute(
      GmcClientService gmcClientService,
      DoctorInfoDto doctor,
      String changeReason,
      String designatedBodyCode
  );

  /**
   * Get the connection request type for this operation.
   *
   * @return the connection request type (ADD or REMOVE)
   */
  ConnectionRequestType getOperationType();
}

