package mobi.eyeline.ips.service.deliveries;

import mobi.eyeline.ips.model.DeliverySubscriber;
import mobi.eyeline.ips.repository.InvitationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;

class FetchThread extends LoopThread {

  private static final Logger logger = LoggerFactory.getLogger(FetchThread.class);

  private final InvitationDeliveryRepository invitationDeliveryRepository;
  private final BlockingQueue<DeliveryWrapper> toFetch;

  public FetchThread(String name,
                     InvitationDeliveryRepository invitationDeliveryRepository,
                     BlockingQueue<DeliveryWrapper> queue) {
    super(name);

    this.invitationDeliveryRepository = invitationDeliveryRepository;
    this.toFetch = queue;
  }

  @Override
  protected void loop() throws InterruptedException {
    final DeliveryWrapper delivery = toFetch.take();

    try {
      doProcess(delivery);

    } catch (Exception e) {
      logger.error("Delivery-" + delivery.getModel().getId() + ": process failed. Cause: " + e.getMessage(), e);
      toFetch.put(delivery);
    }
  }

  private void doProcess(DeliveryWrapper delivery) {
    if (!delivery.shouldBeFilled()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Delivery-" + delivery.getModel().getId() + ": delivery is full, no messages fetched");
      }
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Delivery-" + delivery.getModel().getId() + ": Trying to fetch messages...");
    }

    final List<DeliverySubscriber> subscribers =
        invitationDeliveryRepository.fetchNext(
            delivery.getModel(),
            delivery.getMessagesQueueSize());

    if (subscribers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Delivery-" + delivery.getModel().getId() + ": no messages fetched. Delivery marked as empty.");
      }
      onCompleted(delivery);

    } else {


      for (DeliverySubscriber subscriber : subscribers) {
        final DeliveryWrapper.Message message =
            new DeliveryWrapper.Message(subscriber.getId(), subscriber.getMsisdn());
        delivery.put(message);
      }

      if (logger.isDebugEnabled()) {
        final StringBuilder fetchedIds = new StringBuilder();
        for (DeliverySubscriber subscriber : subscribers) {
          if (fetchedIds.length() > 0)
            fetchedIds.append(",");
          fetchedIds.append(subscriber.getId());
        }
        logger.debug("Delivery-" + delivery.getModel().getId() + ": " + subscribers.size() + " messages fetched. [" + fetchedIds.toString() + "]");
      }
      onAfterFetch(delivery);
    }
  }

  private void onAfterFetch(DeliveryWrapper delivery) {
    // Nothing here.
  }

  private void onCompleted(DeliveryWrapper delivery) {
    delivery.setEmpty();
  }
}
