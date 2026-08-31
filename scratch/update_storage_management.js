const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/StorageManagement.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useTheme } from \'../context/ThemeContext\';',
    'import { useTheme } from \'../context/ThemeContext\';\nimport { useTranslation } from \'../context/LanguageContext\';'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const StorageManagement = () => {',
    'const StorageManagement = () => {\n  const { t } = useTranslation();'
  );
}

content = content.replace(
  '>Storage Management</h2>',
  '>{t("storage.title", "Storage Management")}</h2>'
);

fs.writeFileSync(path, content, 'utf8');
console.log("StorageManagement.jsx updated successfully!");
