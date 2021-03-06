var page = {

  INVALID_DATE_MSG: 'Invalid date format',

  init: function () {
    /* Nothing here */
  },

  onShow: function () {
    var $periodStart = $('#periodStart'),
        $periodEnd = $('#periodEnd'),

        checkFormat = function ($input) {
          if (ips.utils.isDate($input.val())) return true;

          $input.addClass('validationError');
          ips.message.error(page.INVALID_DATE_MSG);
          return false;
        },

        error = false;

    ips.message.hideAll();
    if (!checkFormat($periodStart)) error = true;
    if (!checkFormat($periodEnd))   error = true;

    return !error;
  }
};
