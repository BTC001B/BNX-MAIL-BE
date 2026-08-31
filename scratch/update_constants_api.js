const fs = require('fs');

const constantsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/Data/constants.jsx';
let constantsContent = fs.readFileSync(constantsPath, 'utf8');

if (!constantsContent.includes('SETTINGS:')) {
  constantsContent = constantsContent.replace(
    'USERS: {',
    'SETTINGS: {\n    LANGUAGE: \'/api/settings/language\'\n  },\n  USERS: {'
  );
  fs.writeFileSync(constantsPath, constantsContent, 'utf8');
  console.log("Updated constants.jsx with SETTINGS.LANGUAGE");
} else {
  console.log("constants.jsx already has SETTINGS");
}

const apiPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';
let apiContent = fs.readFileSync(apiPath, 'utf8');

if (!apiContent.includes('settingsAPI')) {
  const settingsApiCode = `
// Settings APIs
export const settingsAPI = {
    getLanguage: () => api.get(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language'),
    updateLanguage: (language) => api.put(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language', { language }),
};
`;
  apiContent += settingsApiCode;
  fs.writeFileSync(apiPath, apiContent, 'utf8');
  console.log("Updated api.js with settingsAPI");
} else {
  console.log("api.js already has settingsAPI");
}
