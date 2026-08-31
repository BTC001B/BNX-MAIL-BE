const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Inbox.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useAuth } from "../context/AuthContext";',
    'import { useAuth } from "../context/AuthContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const Inbox = ({ searchQuery }) => {',
    'const Inbox = ({ searchQuery }) => {\n  const { t } = useTranslation();'
  );
}

content = content.replace(
  '>Inbox</h2>',
  '>{t("inbox.title", "Inbox")}</h2>'
);

content = content.replace(
  '>All Inbox</h2>',
  '>{t("sidebar.all_inbox", "All Inbox")}</h2>'
);

fs.writeFileSync(path, content, 'utf8');
console.log("Inbox.jsx updated successfully!");
