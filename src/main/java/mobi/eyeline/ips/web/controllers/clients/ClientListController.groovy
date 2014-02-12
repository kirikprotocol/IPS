package mobi.eyeline.ips.web.controllers.clients

import mobi.eyeline.ips.exceptions.LoginException
import mobi.eyeline.ips.model.Role
import mobi.eyeline.ips.model.User
import mobi.eyeline.ips.repository.UserRepository
import mobi.eyeline.ips.service.MailService
import mobi.eyeline.ips.service.Services
import mobi.eyeline.ips.service.UserService
import mobi.eyeline.ips.util.HashUtils
import mobi.eyeline.ips.web.controllers.BaseController
import mobi.eyeline.util.jsf.components.data_table.model.DataTableModel
import mobi.eyeline.util.jsf.components.data_table.model.DataTableSortOrder

/**
 * Created by dizan on 05.02.14.
 */
class ClientListController extends BaseController {

    private final UserRepository userRepository = Services.instance().userRepository
    private final UserService userService = Services.instance().userService
    private final MailService mailService = Services.instance().mailService

    def String userLogin
    def String userLoginForEdit

    def String userEmail

    def User userForEdit
    def User newUser

    String search
    Boolean blockError
    Boolean unblockError

    boolean newUserDataValidationError
    boolean modifiedUserDataValidationError
    boolean emailExists
    boolean loginExists
    boolean editedEmailExists
    boolean editedLoginExists
    Boolean passwordResetError

    ClientListController() {
        userForEdit= new User()
        userForEdit.fullName=""
        userForEdit.company=""
        userForEdit.login=""
        userForEdit.email=""

        newUser= new User()
        newUser.fullName=""
        newUser.company=""
        newUser.login=""
        newUser.email=""
    }

    public DataTableModel getTableModel() {
        return new DataTableModel() {

            @Override
            List getRows(int offset,
                         int limit,
                         DataTableSortOrder sortOrder){
                def list = userRepository.list(
                        search,
                        sortOrder.columnId,
                        sortOrder.asc,
                        limit,
                        offset
                )

                return list.collect {
                    new TableItem(
                            fullname: it.fullName,
                            company: it.company,
                            login: it.login,
                            email: it.email,
                            isBlocked: it.blocked
                    )
                }
            }

            @Override
            public int getRowsCount() {
                userRepository.count(search)
            }
        }
    }

    //TODO: validation
    void saveModifiedUser() {
        User user = userRepository.getByLogin(userLoginForEdit)
        String oldEmail = user.email
        String oldLogin = user.login

        user.fullName = userForEdit.fullName
        user.company = userForEdit.company
        user.login = userForEdit.login
        user.email = userForEdit.email

        editedEmailExists = userService.isEmailExists(user.email, user.id)
        editedLoginExists = userService.isLoginExists(user.login, user.id)
        modifiedUserDataValidationError =
                renderViolationMessage(validator.validate(user),
                        [
                                'fullName': 'clientSettingsFullName',
                                'company': 'clientSettingsCompany',
                                'login': 'clientSettingsLogin',
                                'email': 'clientSettingsEmail',
                        ])


        if(editedLoginExists){
            addErrorMessage(getResourceBundle().getString("client.dialog.validation.login.exists"),
                    "clientSettingsLogin")
        }

        if(editedEmailExists){
            addErrorMessage(getResourceBundle().getString("client.dialog.validation.email.exists"),
                    "clientSettingsEmail")
        }

        if (modifiedUserDataValidationError || editedEmailExists || editedLoginExists) {
            return
        }

        userRepository.update(user)

        if (oldLogin != user.login && oldEmail == user.email) {
            mailService.sendUserModified(user)
        }

        if (oldEmail != user.email) {
            mailService.sendUserModified(user,oldEmail)
        }
    }

    void createUser() {
        String password = userService.generatePassword()
        User user = new User(
                fullName: newUser.fullName,
                company: newUser.company,
                login: newUser.login,
                email: newUser.email,
                password: HashUtils.hashPassword(password),
                role: Role.CLIENT)

        emailExists = userService.isEmailExists(user.email)
        loginExists = userService.isLoginExists(user.login)
        newUserDataValidationError =
                renderViolationMessage(validator.validate(user),
                [
                        'fullName': 'newClientFullName',
                        'company': 'newClientCompany',
                        'login': 'newClientLogin',
                        'email': 'newClientEmail',
                ])


        if(loginExists){
            addErrorMessage(getResourceBundle().getString("client.dialog.validation.login.exists"),
                            "newClientLogin")
        }

        if(emailExists){
            addErrorMessage(getResourceBundle().getString("client.dialog.validation.email.exists"),
                            "newClientEmail")
        }

        if(newUserDataValidationError || loginExists || emailExists){
            return
        }

        userRepository.save(user)
        mailService.sendUserRegistration(user, password)
    }


    void blockUser() {
        String userLogin = getParamValue("userLogin").asString()
            try {
                userService.blockUser(userLogin)
                blockError = false
            } catch (LoginException e) {
                blockError = true
            }
    }

    void unblockUser() {
        String userLogin = getParamValue("userLogin").asString()
            try {
                userService.unblockUser(userLogin)
                unblockError = false
            } catch (LoginException e) {
                unblockError = true
            }
    }

    void resetPassword() {
        try {
            userService.restorePassword(userEmail);
            passwordResetError = false
        } catch (LoginException e) {
            passwordResetError = true
        }
    }


    static class TableItem implements Serializable {
        int id
        String fullname
        String company
        String login
        String email
        boolean isBlocked

    }
}
