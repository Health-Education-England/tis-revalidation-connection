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

package uk.nhs.hee.tis.revalidation.connection.mapper;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.connection.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;

@Slf4j
@Component
public class ConnectionInfoMapperHelper {

  private ConnectionInfoMapper connectionInfoMapper;

  public ConnectionInfoMapperHelper(
      ConnectionInfoMapper connectionInfoMapper
  ) {
    this.connectionInfoMapper = connectionInfoMapper;
  }

  /**
   * Maps a subclass instance of BaseConnectionView to ConnectionInfoDto
   *
   * @param view subclass instance of BaseConnectionView
   */
  public <T extends BaseConnectionView> ConnectionInfoDto toDto(T view)
      throws IllegalArgumentException {
    if (view instanceof ConnectedView) {
      return connectionInfoMapper.connectedToDto((ConnectedView) view);
    }
    else if (view instanceof DisconnectedView) {
      return connectionInfoMapper.disconnectedToDto((DisconnectedView) view);
    }
    else if (view instanceof ExceptionView) {
      return connectionInfoMapper.exceptionToDto((ExceptionView) view);
    }
    else {
      throw new IllegalArgumentException("ConnectionInfoMapperHelper: view type not supported");
    }
  }

  /**
   * Maps subclass instances of BaseConnectionView to ConnectionInfoDtos
   *
   * @param views List of subclass instances of BaseConnectionView
   */
  public <T extends BaseConnectionView> List<ConnectionInfoDto> toDtos(List<T> views)
      throws IllegalArgumentException {
    if (views.get(0) instanceof ConnectedView) {
      return connectedToDtos(views);
    }
    else if (views.get(0) instanceof DisconnectedView) {
      return disconnectedToDtos(views);
    }
    else if (views.get(0) instanceof ExceptionView) {
      return exceptionToDtos(views);
    }
    else {
      throw new IllegalArgumentException("ConnectionInfoMapperHelper: view type not supported");
    }
  }

  private <T extends BaseConnectionView> List<ConnectionInfoDto> connectedToDtos(List<T> views) {
    List<ConnectionInfoDto> dtos = new ArrayList<>();
    for (BaseConnectionView view: views) {
      dtos.add(connectionInfoMapper.connectedToDto((ConnectedView) view));
    }
    return dtos;
  }

  private <T extends BaseConnectionView> List<ConnectionInfoDto> disconnectedToDtos(List<T> views) {
    List<ConnectionInfoDto> dtos = new ArrayList<>();
    for (BaseConnectionView view: views) {
      dtos.add(connectionInfoMapper.disconnectedToDto((DisconnectedView) view));
    }
    return dtos;
  }

  private <T extends BaseConnectionView> List<ConnectionInfoDto> exceptionToDtos(List<T> views) {
    List<ConnectionInfoDto> dtos = new ArrayList<>();
    for (BaseConnectionView view: views) {
      dtos.add(connectionInfoMapper.exceptionToDto((ExceptionView) view));
    }
    return dtos;
  }


}
