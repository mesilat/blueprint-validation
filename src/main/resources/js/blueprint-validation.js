define('com.mesilat.vbp.preinit', [], function(){
  var initRteCalled = false;

  return {
    setInitRteCalled: function () {
      initRteCalled = true;
    },
    getInitRteCalled: function () {
      return initRteCalled;
    }
  };
});

require('confluence/module-exporter')
.safeRequire('com.mesilat.vbp.preinit', function(preinit) {
  // Early "init.rte" hook
  AJS.bind('init.rte', function(){
    preinit.setInitRteCalled();
  });
});
