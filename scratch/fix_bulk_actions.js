const fs = require('fs');
const { execSync } = require('child_process');

execSync('git checkout -- src/components/BulkActionsToolbar.jsx', { cwd: 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE' });

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/BulkActionsToolbar.jsx';
let content = fs.readFileSync(path, 'utf8');

if (!content.includes('useTranslation')) {
  content = `import { useTranslation } from "../context/LanguageContext";\n${content}`;
}

if (!content.includes('const { t } = useTranslation();')) {
  const match = content.match(/(const BulkActionsToolbar = \([^)]*\) => \{)/);
  if (match) {
    const idx = content.indexOf(match[0]) + match[0].length;
    content = content.slice(0, idx) + '\n  const { t } = useTranslation();' + content.slice(idx);
  }
}

content = content.replace('title="Report Spam"', 'title={t("bulk_actions.report_spam", "Report Spam")}');
content = content.replace('title="Select All"', 'title={t("bulk_actions.select_all", "Select All")}');
content = content.replace('title="Deselect All"', 'title={t("bulk_actions.deselect_all", "Deselect All")}');
content = content.replace('title="Mark as read"', 'title={t("bulk_actions.mark_read", "Mark as read")}');
content = content.replace('title="Mark as unread"', 'title={t("bulk_actions.mark_unread", "Mark as unread")}');
content = content.replace('title="Delete Selected"', 'title={t("bulk_actions.delete_selected", "Delete Selected")}');
content = content.replace('title="Archive Selected"', 'title={t("bulk_actions.archive_selected", "Archive Selected")}');

fs.writeFileSync(path, content, 'utf8');
console.log("Fixed BulkActionsToolbar.jsx cleanly!");
