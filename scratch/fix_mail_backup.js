const fs = require('fs');
const { execSync } = require('child_process');

execSync('git checkout -- src/pages/MailBackup.jsx', { cwd: 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE' });

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/MailBackup.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = `import { useTranslation } from "../context/LanguageContext";\n${content}`;
}

// Inject hook inside component
if (!content.includes('const { t } = useTranslation();')) {
  content = content.replace('export default function MailBackup() {', 'export default function MailBackup() {\n  const { t } = useTranslation();');
}

// Replace title inside JSX cleanly
content = content.replace(
  '<h1 className="text-xl font-bold">Mail Backup</h1>',
  '<h1 className="text-xl font-bold">{t("mail_backup.title", "Mail Backup")}</h1>'
);
content = content.replace(
  '<button onClick={handleBackupNow}',
  '<button onClick={handleBackupNow} title={t("mail_backup.backup_now", "Backup Now")}'
);

fs.writeFileSync(path, content, 'utf8');
console.log("Fixed MailBackup.jsx cleanly!");
