define('com.mesilat.vbp.locale-strings', [], function(){
  const strings = {
    'vbp.msg.common.success': AJS.I18n.getText('vbp.msg.common.success'),
    'vbp.msg.common.error': AJS.I18n.getText('vbp.msg.common.error'),
    'vbp.msg.template.save.success': AJS.I18n.getText('vbp.msg.template.save.success'),
    'vbp.msg.template.upload.success': AJS.I18n.getText('vbp.msg.template.upload.success')
  };

  return {
    getLocaleStrings: function() {
      return strings;
    }
  }
});
