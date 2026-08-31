const fs = require('fs');
const appPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/App.jsx';
let content = fs.readFileSync(appPath, 'utf8');

if (!content.includes('LanguageProvider')) {
  content = content.replace(
    'import { SignupProvider } from "./context/SignupContext";',
    'import { SignupProvider } from "./context/SignupContext";\nimport { LanguageProvider } from "./context/LanguageContext";'
  );

  content = content.replace(
    '<AuthProvider>',
    '<AuthProvider>\n    <LanguageProvider>'
  );

  content = content.replace(
    '</AuthProvider>',
    '  </LanguageProvider>\n    </AuthProvider>'
  );

  fs.writeFileSync(appPath, content, 'utf8');
  console.log("App.jsx updated with LanguageProvider successfully!");
} else {
  console.log("App.jsx already has LanguageProvider.");
}
