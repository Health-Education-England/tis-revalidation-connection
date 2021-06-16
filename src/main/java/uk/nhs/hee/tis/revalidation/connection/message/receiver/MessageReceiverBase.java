package uk.nhs.hee.tis.revalidation.connection.message.receiver;

public abstract class MessageReceiverBase<T> {

  public abstract void handleMessage(T message);
}
