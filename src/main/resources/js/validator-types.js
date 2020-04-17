define('com.mesilat.vbp.validator-types', [], function(){
  const VALIDATOR_TYPES = {
    LOFV: {
      id: 'LOFV',
      name: AJS.I18n.getText('com.mesilat.vbp.types.LOFV')
    },
    REXP: {
      id: 'REXP',
      name: AJS.I18n.getText('com.mesilat.vbp.types.REXP')
    },
    NUMR: {
      id: 'NUMR',
      name: AJS.I18n.getText('com.mesilat.vbp.types.NUMR')
    },
    USER: {
      id: 'USER',
      name: AJS.I18n.getText('com.mesilat.vbp.types.USER')
    },
    PAGE: {
      id: 'PAGE',
      name: AJS.I18n.getText('com.mesilat.vbp.types.PAGE')
    },
    DATE: {
      id: 'DATE',
      name: AJS.I18n.getText('com.mesilat.vbp.types.DATE')
    },
    MODL: {
      id: 'MODL',
      name: AJS.I18n.getText('com.mesilat.vbp.types.MODL')
    }
  };

  return {
    getValidatorTypes: function () {
      return VALIDATOR_TYPES
    }
  };
});
