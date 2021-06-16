package uk.nhs.hee.tis.revalidation.connection.message.receiver;

public interface MessageReceiver<T> {

  void handleMessage(T message);
}
