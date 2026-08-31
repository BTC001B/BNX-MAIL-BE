const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/MailBackup.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useAuth } from "../context/AuthContext";',
    'import { useAuth } from "../context/AuthContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const MailBackup = () => {',
    'const MailBackup = () => {\n  const { t } = useTranslation();'
  );
}

content = content.replace(
  '<h1 className="text-2xl font-bold">Mail Backup</h1>',
  '<h1 className="text-2xl font-bold">{t("mail_backup.title", "Mail Backup")}</h1>'
);

content = content.replace(
  '>Mail Backup</h1>',
  '>{t("mail_backup.title", "Mail Backup")}</h1>'
);

fs.writeFileSync(path, content, 'utf8');
console.log("MailBackup.jsx updated successfully!");
