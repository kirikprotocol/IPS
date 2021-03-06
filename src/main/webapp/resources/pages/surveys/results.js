var page = {

  INVALID_DATE_MSG: 'Invalid date format',

  init: function () {
    var onRowsUpdate = function () {
      var hasResults = jsfc('resultsTable').getRowsCount() != 0;
      $('#downloadButtonsPanel').toggle(hasResults);
    };

    onRowsUpdate();

    jsfc('resultsTable').bind('update', onRowsUpdate);
  },

  filterKeyDown: function (event) {
    if (event.keyCode == 13) {
      return page.onSearch();
    } else {
      return true;
    }
  },

  onSearch : function () {
    if (page._checkDatesValid()) {
      jsfc('resultsTable').update(true);
    }

    return false;
  },

  /**
   * @return {boolean}  True iff date inputs contain valid values.
   * @private
   */
  _checkDatesValid: function () {
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
  },

  onResultsDeleteNoResults: function() {
    jsfc('resultsDeleteNoResultsDialog').show();
  },

  onResultsDeleteConfirmation: function() {
    jsfc('resultsDeleteConfirmationDialog').show();
  }
};
