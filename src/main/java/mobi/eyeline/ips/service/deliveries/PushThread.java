package mobi.eyeline.ips.service.deliveries;

import com.google.common.base.Throwables;
import mobi.eyeline.ips.model.InvitationDelivery;
import mobi.eyeline.ips.repository.DeliverySubscriberRepository;
import mobi.eyeline.ips.repository.InvitationDeliveryRepository;
import mobi.eyeline.ips.service.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import static mobi.eyeline.ips.service.deliveries.DeliveryWrapper.DelayedDeliveryWrapper;

class PushThread extends LoopThread {

    private static final Logger logger = LoggerFactory.getLogger(PushThread.class);

    private static final long WAIT_TO_FILL = TimeUnit.SECONDS.toMillis(2);

    private final DelayQueue<DelayedDeliveryWrapper> toSend;
    private final BlockingQueue<DeliveryWrapper> toFetch;
    private final BlockingQueue<DeliveryWrapper.Message> toMark;

    private final DeliveryPushService deliveryPushService;
    private final DeliverySubscriberRepository deliverySubscriberRepository;
    private final InvitationDeliveryRepository invitationDeliveryRepository;

    public PushThread(String name,

                      DelayQueue<DelayedDeliveryWrapper> toSend,
                      BlockingQueue<DeliveryWrapper> toFetch,
                      BlockingQueue<DeliveryWrapper.Message> toMark,

                      DeliveryPushService deliveryPushService,
                      DeliverySubscriberRepository deliverySubscriberRepository,
                      InvitationDeliveryRepository invitationDeliveryRepository) {

        super(name);

        this.toSend = toSend;
        this.toFetch = toFetch;
        this.toMark = toMark;

        this.deliveryPushService = deliveryPushService;
        this.deliverySubscriberRepository = deliverySubscriberRepository;
        this.invitationDeliveryRepository = invitationDeliveryRepository;
    }

    @Override
    protected void loop() throws InterruptedException {
        final DeliveryWrapper delivery = toSend.take().getDeliveryWrapper();

        if (delivery.isStopped()) {
            // Just removed from the queue, so it's OK.
            logger.info("Delivery is stopped, throwing out [" + delivery + "]");

        } else {
            final DeliveryWrapper.Message message = delivery.poll();
            if (message == null) {
                handleEmpty(delivery);

            } else {
                handleMessage(delivery, message);
            }

            if (delivery.shouldBeFilled()) {
                toFetch.put(delivery);
            }
        }
    }

    private void handleEmpty(DeliveryWrapper delivery) {
        if (logger.isTraceEnabled()) {
            logger.trace("Delivery: [" + delivery + "] is empty");
        }

        if (delivery.isEmpty()) {
            // Fetch process marked this one as having no more entries in DB,
            // so just update the state accordingly.
            final InvitationDelivery dbModel =
                    invitationDeliveryRepository.load(delivery.getModel().getId());
            dbModel.setState(InvitationDelivery.State.COMPLETED);
            invitationDeliveryRepository.update(dbModel);

            // XXX:DEBUG
            Services.instance().getDeliveryService().onDeliveryKick(delivery);

        } else {
            toSend.put(DelayedDeliveryWrapper.forDelay(delivery, WAIT_TO_FILL));
        }
    }

    private void handleMessage(DeliveryWrapper delivery,
                               DeliveryWrapper.Message message) throws InterruptedException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Delivery: [" + delivery + "], message = [" + message + "]");
            }
            doHandleMessage(delivery, message);

        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InterruptedException.class);
            logger.error("Processing failed, " +
                    "delivery = [" + delivery + "], message = [" + message + "]");
        }
    }

    private void doHandleMessage(DeliveryWrapper delivery,
                                 DeliveryWrapper.Message message) throws InterruptedException {
        try {
            try {
                doPush(delivery, message);
            } finally {
                toSend.put(DelayedDeliveryWrapper.forSent(delivery));
            }

            doMark(message, true);

        } catch (IOException e) {
            logger.warn("Message sending failed", e);
            doMark(message, false);
        }
    }

    private void doPush(DeliveryWrapper next,
                        DeliveryWrapper.Message message) throws IOException {
        final InvitationDelivery.Type type = next.getModel().getType();
        switch (type) {
            case USSD_PUSH:
                deliveryPushService.pushUssd(
                        message.getId(),
                        message.getMsisdn(),
                        next.getModel().getText());
                break;
            case NI_DIALOG:
                deliveryPushService.niDialog(
                        message.getId(),
                        message.getMsisdn(),
                        next.getModel().getId());
                break;
            default:
                throw new AssertionError("Unknown delivery type: " + type);
        }
    }

    private void doMark(DeliveryWrapper.Message message,
                        boolean success) throws InterruptedException {
        toMark.put(message.setSent(success));
    }
}
