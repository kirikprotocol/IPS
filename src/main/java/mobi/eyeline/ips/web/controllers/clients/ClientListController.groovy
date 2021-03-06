package mobi.eyeline.ips.web.controllers.clients

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import mobi.eyeline.ips.exceptions.LoginException
import mobi.eyeline.ips.model.Role
import mobi.eyeline.ips.model.User
import mobi.eyeline.ips.repository.UserRepository
import mobi.eyeline.ips.service.MailService
import mobi.eyeline.ips.service.TimeZoneService
import mobi.eyeline.ips.service.UserService
import mobi.eyeline.ips.util.HashUtils
import mobi.eyeline.ips.web.controllers.BaseController
import mobi.eyeline.ips.web.controllers.TimeZoneHelper
import mobi.eyeline.util.jsf.components.data_table.model.DataTableModel
import mobi.eyeline.util.jsf.components.data_table.model.DataTableSortOrder

import javax.enterprise.inject.Model
import javax.faces.model.SelectItem
import javax.inject.Inject

@CompileStatic
@Slf4j('logger')
@Model
class ClientListController extends BaseController {

  @Inject private UserRepository userRepository
  @Inject private UserService userService
  @Inject private MailService mailService
  @Inject private TimeZoneService timeZoneService

  String search
  Boolean dialogForEdit

  // User ID for creation/modification dialog, null if creating new account.
  Integer modifiedUserId
  User userForEdit
  boolean modifiedUserDataValidationError

  Boolean blockError
  Boolean unblockError
  Boolean passwordResetError

  ClientListController() {
    userForEdit = new User()
  }

  DataTableModel getTableModel() {
    return new DataTableModel() {

      private User getOwner() {
        assert getCurrentUser().role == Role.MANAGER
        return getCurrentUser().showAllClients ? null : getCurrentUser()
      }

      @Override
      List getRows(int offset,
                   int limit,
                   DataTableSortOrder sortOrder) {
        def list = userRepository.list(
            this.owner,
            search,
            sortOrder.columnId,
            sortOrder.asc,
            limit,
            offset
        )

        return list.collect {
          new TableItem(
              id: it.id,
              fullName: it.fullName,
              company: it.company,
              login: it.login,
              email: it.email,
              blocked: it.blocked
          )
        }
      }

      @Override
      int getRowsCount() {
        userRepository.count(this.owner, search)
      }
    }
  }

  void fillUserForEdit() {
    modifiedUserId = getParamValue("userForEditId").asInteger()
    if (modifiedUserId != null) {
      userForEdit = userRepository.get(modifiedUserId)
      dialogForEdit = true
    } else {
      userForEdit = new User()
      dialogForEdit = false
    }
  }

  void saveModifiedUser() {
    if (modifiedUserId == null) {
      // Create a new user account.
      String password = userService.generatePassword()
      User user = new User(
          fullName: userForEdit.fullName,
          company: userForEdit.company,
          login: userForEdit.login,
          email: userForEdit.email,
          password: HashUtils.hashPassword(password),
          locale: userForEdit.locale,
          timeZoneId: userForEdit.timeZoneId,
          role: Role.CLIENT,
          manager: currentUser)

      if (validate(user)) {
        userRepository.save(user)
        mailService.sendUserRegistration(user, password)
      }

    } else {
      // Modify existing account.
      User user = userRepository.load(modifiedUserId)
      String oldEmail = user.email
      String oldLogin = user.login

      user.with {
        fullName = userForEdit.fullName
        company = userForEdit.company
        login = userForEdit.login
        email = userForEdit.email
        it.locale = userForEdit.locale
        timeZoneId = userForEdit.timeZoneId
      }

      if (validate(user)) {
        userRepository.update(user)

        if (oldLogin != user.login && oldEmail == user.email) {
          mailService.sendUserModified(user)
        }

        if (oldEmail != user.email) {
          mailService.sendUserModified(user, oldEmail)
        }
      }
    }
  }

  private boolean validate(User user) {
    modifiedUserDataValidationError =
        renderViolationMessage(validator.validate(user), [
            'fullName': 'clientSettingsFullName',
            'company' : 'clientSettingsCompany',
            'login'   : 'clientSettingsLogin',
            'email'   : 'clientSettingsEmail',
        ], ['fullName', 'company', 'login', 'email'])

    if (!userService.isLoginAllowed(user)) {
      addErrorMessage(strings['client.dialog.validation.login.exists'], 'clientSettingsLogin')
      modifiedUserDataValidationError = true
    }

    if (!userService.isEmailAllowed(user)) {
      addErrorMessage(strings['client.dialog.validation.email.exists'], 'clientSettingsEmail')
      modifiedUserDataValidationError = true
    }

    return !modifiedUserDataValidationError
  }

  void deActivateUser() {
    try {
      userService.deActivate(userRepository.load(modifiedUserId))
      blockError = false
    } catch (Exception e) {
      logger.error("Error activating account", e)
      blockError = true
    }
  }

  void activateUser() {
    try {
      userService.activate(userRepository.load(modifiedUserId))
      unblockError = false
    } catch (Exception e) {
      logger.error("Error deactivating account", e)
      unblockError = true
    }
  }

  void resetPassword() {
    try {
      userService.resetPassword(userRepository.load(modifiedUserId))
      passwordResetError = false

    } catch (LoginException e) {
      logger.error("Error in password reset", e)
      passwordResetError = true
    }
  }

  List<SelectItem> getTimeZones() { TimeZoneHelper.getTimeZones(timeZoneService, getLocale()) }

  static class TableItem implements Serializable {
    int id
    String fullName
    String company
    String login
    String email
    boolean blocked
  }
}
