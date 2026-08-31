const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
  'import { emailAPI, authAPI, userAPI, signatureAPI } from "../services/api";',
  'import { emailAPI, authAPI, userAPI, signatureAPI, settingsAPI } from "../services/api";'
);

fs.writeFileSync(path, content, 'utf8');
console.log("Replaced import line in Settings.jsx successfully!");
