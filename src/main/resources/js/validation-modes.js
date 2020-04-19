define('com.mesilat.vbp.validation-modes', [], function(){
  const VALIDATION_MODES = {
    NONE: {
      id: 'NONE',
      name: AJS.I18n.getText('com.mesilat.vbp.template.mode.NONE')
    },
    WARN: {
      id: 'WARN',
      name: AJS.I18n.getText('com.mesilat.vbp.template.mode.WARN')
    },
    FAIL: {
      id: 'FAIL',
      name: AJS.I18n.getText('com.mesilat.vbp.template.mode.FAIL')
    }
  };

  return {
    getValidationModes: function () {
      return VALIDATION_MODES
    }
  };
});
