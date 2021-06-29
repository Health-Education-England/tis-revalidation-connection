package uk.nhs.hee.tis.revalidation.connection.mapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.connection.entity.BaseConnectionView;
import uk.nhs.hee.tis.revalidation.connection.entity.ConnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.DisconnectedView;
import uk.nhs.hee.tis.revalidation.connection.entity.ExceptionView;


@ExtendWith(MockitoExtension.class)
public class ConnectionInfoMapperHelperTest {
  @Mock
  ConnectionInfoMapper connectionInfoMapper;
  @InjectMocks
  ConnectionInfoMapperHelper connectionInfoMapperHelper;

  private ConnectedView connectedView;
  private DisconnectedView disconnectedView;
  private ExceptionView exceptionView;
  private List<ConnectedView> connectedViews;
  private List<DisconnectedView> disconnectedViews;
  private List<ExceptionView> exceptionViews;

  @BeforeEach
  public void setup() {
    initializeViews();
    initializeViewLists();
  }

  @Test
  void shouldMapToConnectedView() {
    connectionInfoMapperHelper.toDto(connectedView);
    verify(connectionInfoMapper).connectedToDto(connectedView);
  }

  @Test
  void shouldMapToDisconnectedView() {
    connectionInfoMapperHelper.toDto(disconnectedView);
    verify(connectionInfoMapper).disconnectedToDto(disconnectedView);
  }

  @Test
  void shouldMapToExceptionView() {
    connectionInfoMapperHelper.toDto(exceptionView);
    verify(connectionInfoMapper).exceptionToDto(exceptionView);
  }

  @Test
  void shouldMapToConnectedViews() {
    connectionInfoMapperHelper.toDtos(connectedViews);
    verify(connectionInfoMapper).connectedToDto(connectedView);
  }

  @Test
  void shouldMapToDisconnectedViews() {
    connectionInfoMapperHelper.toDtos(disconnectedViews);
    verify(connectionInfoMapper).disconnectedToDto(disconnectedView);
  }

  @Test
  void shouldMapToExceptionViews() {
    connectionInfoMapperHelper.toDtos(exceptionViews);
    verify(connectionInfoMapper).exceptionToDto(exceptionView);
  }

  @Test
  void shouldThrowExceptionIfViewNotSupported() {
    assertThrows(IllegalArgumentException.class, () -> {
      connectionInfoMapperHelper.toDto(BaseConnectionView.builder().build());
    });
  }

  @Test
  void shouldThrowExceptionIfViewsNotSupported() {
    List<BaseConnectionView> unsupportedViews = new ArrayList<>();
    unsupportedViews.add(BaseConnectionView.builder().build());
    assertThrows(IllegalArgumentException.class, () -> {
      connectionInfoMapperHelper.toDtos(unsupportedViews);
    });
  }

  private void initializeViews() {
    connectedView = ConnectedView.builder().build();
    disconnectedView = DisconnectedView.builder().build();
    exceptionView =ExceptionView.builder().build();
  }

  private void initializeViewLists() {
    connectedViews = new ArrayList<>();
    connectedViews.add(connectedView);
    disconnectedViews = new ArrayList<>();
    disconnectedViews.add(disconnectedView);
    exceptionViews = new ArrayList<>();
    exceptionViews.add(exceptionView);
  }
}
