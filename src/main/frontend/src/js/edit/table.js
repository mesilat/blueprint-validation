import { OBJECTID_PREFIX, OBJECT_CLASS } from "../constants";
import { generateUUID, trace } from "../util";
import tinymce from "tinymce";

function copyObjectClasses($src, $dst) {
  const classes = [];
  $src[0].classList.forEach(className => {
    if (!className.startsWith(OBJECTID_PREFIX)){
      classes.push(className);
    }
  });
  classes.push(`${OBJECTID_PREFIX}${generateUUID()}`);
  $dst.removeClass().addClass(classes.join(' '));
}

export default () => {
  trace("table::");
  //var tinymce = require('tinymce');
  tinymce.activeEditor.onExecCommand.add((ed, cmd) => {
    switch (cmd){
      case 'mceTablePasteRowBefore':
      case 'mceTableInsertRowBefore':
        {
          const $src = $(ed.selection.getNode()).closest('tr');
          if ($src.hasClass(OBJECT_CLASS)){
            const $dst = $src.prev();
            copyObjectClasses($src, $dst);
          }
        }
        break;
      case 'mceTablePasteRowAfter':
      case 'mceTableInsertRowAfter':
        {
          const $src = $(ed.selection.getNode()).closest('tr');
          if ($src.hasClass(OBJECT_CLASS)){
            const $dst = $src.next();
            copyObjectClasses($src, $dst);
          }
        }
    }
  });
}
