package mobi.eyeline.ips.web.controllers.surveys

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import mobi.eyeline.ips.model.InvitationDelivery

import mobi.eyeline.ips.repository.DeliverySubscriberRepository
import mobi.eyeline.ips.repository.InvitationDeliveryRepository
import mobi.eyeline.ips.service.Services
import mobi.eyeline.ips.util.DeliveryUtils
import mobi.eyeline.ips.util.DeliveryUtils.DuplicateMsisdnException
import mobi.eyeline.ips.util.DeliveryUtils.InvalidMsisdnFormatException
import mobi.eyeline.ips.web.controllers.BaseController
import mobi.eyeline.util.jsf.components.data_table.model.DataTableModel
import mobi.eyeline.util.jsf.components.data_table.model.DataTableSortOrder
import mobi.eyeline.util.jsf.components.input_file.UploadedFile

import javax.faces.model.SelectItem
import java.text.MessageFormat


// TODO-10: If "1.1" is entered in the speed input, dialog closes with "Conversion error"
// TODO-11: "Expand" links in the table don't get proper pointer (see `Results' tab)
@CompileStatic
@Slf4j('logger')
class InvitationDeliveryController extends BaseSurveyController {
    private final InvitationDeliveryRepository invitationDeliveryRepository =
            Services.instance().invitationDeliveryRepository
    private final DeliverySubscriberRepository deliverySubscriberRepository =
            Services.instance().deliverySubscriberRepository

    boolean deliveryModifyError
    Boolean activateError
    Boolean pauseError

    InvitationDelivery invitationDelivery
    Boolean dialogForEdit
    Integer modifiedDeliveryId
    String modifiedDeliveryFilename

    List<String> abonents
    UploadedFile inputFile
    String progress

    InvitationDeliveryController() {
        invitationDelivery = new InvitationDelivery()
    }

    DataTableModel getTableModel() {
        return new DataTableModel() {
            @Override
            List getRows(int offset,
                         int limit,
                         DataTableSortOrder sortOrder) {
                final List<InvitationDelivery> list = invitationDeliveryRepository.list(
                        getSurvey(),
                        sortOrder.columnId,
                        sortOrder.asc,
                        limit,
                        offset)
                return list.collect { InvitationDelivery it ->
                    String type = BaseController.strings["invitations.deliveries.table.type.$it.type".toString()]
                    String statusString = BaseController.strings["invitations.deliveries.table.status.$it.state".toString()]
                    new TableItem(
                            id: it.id,
                            date: it.date,
                            type: type,
                            speed: it.speed,
                            currentPosition: it.processedCount,
                            errorsCount: it.errorsCount,
                            status: it.state,
                            statusString: statusString,
                            text: it.text
                    )
                }
            }

            @Override
            int getRowsCount() {
                return invitationDeliveryRepository.count(getSurvey())
            }
        }
    }

    public void fillDeliveryForEdit() {
        modifiedDeliveryId = getParamValue("modifiedDeliveryId").asInteger()
        if (modifiedDeliveryId != null) {
            invitationDelivery = invitationDeliveryRepository.get(modifiedDeliveryId)
            modifiedDeliveryFilename = invitationDelivery.inputFile
            dialogForEdit = true
        } else {
            invitationDelivery = new InvitationDelivery()
            dialogForEdit = false
        }
    }

    public void saveDelivery() {
        if (modifiedDeliveryId == null) {
            invitationDelivery.survey = getSurvey()
            invitationDelivery.date = new Date()
            invitationDelivery.inputFile = (inputFile != null) ? inputFile.filename : null

            if (validate(invitationDelivery)) {
                if (validate(inputFile)) {
                    invitationDeliveryRepository.save(invitationDelivery, abonents)
                }
            }
        } else {
            InvitationDelivery editedDelivery = invitationDeliveryRepository.load(modifiedDeliveryId)

            editedDelivery.with { InvitationDelivery invDel ->
                invDel.type = invitationDelivery.type
                invDel.text = invitationDelivery.text
                invDel.speed = invitationDelivery.speed
            }

            if (validate(editedDelivery)) {
                invitationDeliveryRepository.update(editedDelivery)
            }

        }

    }

    public void startDelivery(){
        invitationDelivery.state = InvitationDelivery.State.ACTIVE
        saveDelivery()
    }

    private boolean validate(InvitationDelivery invitationDelivery) {

        deliveryModifyError =
                renderViolationMessage(validator.validate(invitationDelivery), [
                        'text': 'invitationText',
                        'speed': 'deliverySpeed',
                        'inputFile': 'deliveryReceivers',
                ])

        return !deliveryModifyError
    }

    private boolean validate(UploadedFile file) {
        FileValidationResult result = validateFile(file)
        if (result.error) {
            addErrorMessage(result.errorMessage, 'deliveryReceivers')
            deliveryModifyError = true
        }

        abonents = result.msisdns

        return !deliveryModifyError
    }

    private FileValidationResult validateFile(UploadedFile file) {
        FileValidationResult validationResult = new FileValidationResult(error: false)
        DeliveryUtils utils = new DeliveryUtils()

        def msisdns = []
        try {
            msisdns = utils.parseFile(inputFile.inputStream)

        } catch (InvalidMsisdnFormatException e1) {
            validationResult.generateInvalidErrorMessage(e1.lineNumber, e1.invalidString)
        } catch (DuplicateMsisdnException e2) {
            validationResult.generateDuplicateErrorMessage(e2.lineNumber, e2.invalidString)
        }
        validationResult.msisdns = msisdns
        return validationResult
    }

    void activateDelivery() {
        try {
            InvitationDelivery editedDelivery = invitationDeliveryRepository.load(modifiedDeliveryId)
            editedDelivery.state = InvitationDelivery.State.ACTIVE
            invitationDeliveryRepository.update(editedDelivery)
            activateError = false

        } catch (Exception e) {
            logger.error("Error activating delivery", e)
            activateError = true
        }
    }

    void pauseDelivery() {
        try {
            InvitationDelivery editedDelivery = invitationDeliveryRepository.load(modifiedDeliveryId)
            editedDelivery.state = InvitationDelivery.State.INACTIVE
            invitationDeliveryRepository.update(editedDelivery)
            pauseError = false

        } catch (Exception e) {
            logger.error("Error pause delivery", e)
            pauseError = true
        }
    }


    List<SelectItem> getTypes() {
        InvitationDelivery.Type.values().collect {
            new SelectItem(it, strings["invitations.deliveries.table.type.$it".toString()])
        }
    }

    static class TableItem implements Serializable {
        int id
        Date date
        String type
        int speed
        int currentPosition
        int errorsCount
        InvitationDelivery.State status
        String statusString
        String text
    }

    static class FileValidationResult {
        boolean error
        String errorMessage
        List<String> msisdns

        void generateInvalidErrorMessage(int lineNumber, String invalidString) {
            error = true
            String bundleMessage = BaseController.strings["invitations.deliveries.dialog.file.error.invalid"]
            errorMessage = MessageFormat.format(
                    bundleMessage,
                    invalidString,
                    lineNumber);
        }

        void generateDuplicateErrorMessage(int lineNumber, String invalidString) {
            error = true
            String bundleMessage = BaseController.strings["invitations.deliveries.dialog.file.error.duplicate"]
            errorMessage = MessageFormat.format(
                    bundleMessage,
                    invalidString,
                    lineNumber);
        }

    }

    static class FileValidateException extends Exception {

    }
}
