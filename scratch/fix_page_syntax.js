const fs = require('fs');
const path = require('path');

const feSrc = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';
const pages = ['Starred', 'Draft', 'Send', 'Trash', 'Spam', 'Snoozed', 'Scheduled', 'Archive', 'AllMail', 'Unread', 'BulkActionsToolbar', 'EmailList', 'StorageCard', 'AppLauncher', 'ContactPanel'];

pages.forEach(p => {
  const isComp = ['BulkActionsToolbar', 'EmailList', 'StorageCard', 'AppLauncher', 'ContactPanel'].includes(p);
  const fileP = isComp ? path.join(feSrc, `components/${p}.jsx`) : path.join(feSrc, `pages/${p}.jsx`);
  if (!fs.existsSync(fileP)) return;

  let content = fs.readFileSync(fileP, 'utf8');

  // Fix syntax in function signature
  content = content.replace(/const (\w+) = \(\{\s*\.\.\.props\s*\}\) => \{\s*const \{ t \} = useTranslation\(\);\s*const \{ ([^}]+)\}\) => \{/g, 'const $1 = ({ $2 }) => {\n  const { t } = useTranslation();');

  fs.writeFileSync(fileP, content, 'utf8');
  console.log(`Fixed syntax in: ${p}.jsx`);
});
