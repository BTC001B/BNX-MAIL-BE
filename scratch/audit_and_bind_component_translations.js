const fs = require('fs');
const path = require('path');

const feSrc = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

function updateComponentTranslations(relPath, replacements) {
  const fullPath = path.join(feSrc, relPath);
  if (!fs.existsSync(fullPath)) return;
  let content = fs.readFileSync(fullPath, 'utf8');

  // Ensure useTranslation is imported
  if (!content.includes('useTranslation')) {
    content = `import { useTranslation } from "../context/LanguageContext";\n${content}`;
  }

  // Ensure const { t } = useTranslation(); exists inside function
  if (!content.includes('const { t } = useTranslation();')) {
    const match = content.match(/(const \w+ = \([^{]*\) => \{|function \w+\([^{]*\) \{|export default function \w+\([^{]*\) \{)/);
    if (match) {
      const idx = content.indexOf(match[0]) + match[0].length;
      content = content.slice(0, idx) + '\n  const { t } = useTranslation();' + content.slice(idx);
    }
  }

  replacements.forEach(({ search, replace }) => {
    content = content.split(search).join(replace);
  });

  fs.writeFileSync(fullPath, content, 'utf8');
  console.log(`Audited & updated translations for: ${relPath}`);
}

// 1. BulkActionsToolbar.jsx
updateComponentTranslations('components/BulkActionsToolbar.jsx', [
  { search: 'Select All', replace: '{t("bulk_actions.select_all", "Select All")}' },
  { search: 'Deselect All', replace: '{t("bulk_actions.deselect_all", "Deselect All")}' },
  { search: 'Mark as read', replace: '{t("bulk_actions.mark_read", "Mark as read")}' },
  { search: 'Mark as unread', replace: '{t("bulk_actions.mark_unread", "Mark as unread")}' },
  { search: 'Delete Selected', replace: '{t("bulk_actions.delete_selected", "Delete Selected")}' },
  { search: 'Report Spam', replace: '{t("bulk_actions.report_spam", "Report Spam")}' },
  { search: 'Archive Selected', replace: '{t("bulk_actions.archive_selected", "Archive Selected")}' }
]);

// 2. EmailDetails.jsx
updateComponentTranslations('components/EmailDetails.jsx', [
  { search: '>Reply<', replace: '>{t("email_details.reply", "Reply")}<' },
  { search: '>Reply All<', replace: '>{t("email_details.reply_all", "Reply All")}<' },
  { search: '>Forward<', replace: '>{t("email_details.forward", "Forward")}<' },
  { search: '>Delete<', replace: '>{t("email_details.delete", "Delete")}<' },
  { search: '>Archive<', replace: '>{t("email_details.archive", "Archive")}<' },
  { search: '>Print<', replace: '>{t("email_details.print", "Print")}<' },
  { search: '>Download<', replace: '>{t("email_details.download", "Download")}<' },
  { search: '>Attachments<', replace: '>{t("email_details.attachments", "Attachments")}<' }
]);

// 3. FloatingCompose.jsx
updateComponentTranslations('components/FloatingCompose.jsx', [
  { search: 'New Message', replace: '{t("compose.new_message", "New Message")}' },
  { search: 'placeholder="To"', replace: 'placeholder={t("compose.to", "To")}' },
  { search: 'placeholder="Subject"', replace: 'placeholder={t("compose.subject", "Subject")}' },
  { search: 'placeholder="Write your email here..."', replace: 'placeholder={t("compose.body_placeholder", "Write your email here...")}' },
  { search: '>Send<', replace: '>{t("common.send", "Send")}<' },
  { search: 'Save Draft', replace: '{t("compose.save_draft", "Save Draft")}' }
]);

// 4. MailBackup.jsx
updateComponentTranslations('pages/MailBackup.jsx', [
  { search: 'Mail Backup', replace: '{t("mail_backup.title", "Mail Backup")}' },
  { search: 'Backup Now', replace: '{t("mail_backup.backup_now", "Backup Now")}' },
  { search: 'Download Backup', replace: '{t("mail_backup.download", "Download Backup")}' },
  { search: 'Restore Backup', replace: '{t("mail_backup.restore", "Restore Backup")}' }
]);

// 5. StorageManagement.jsx
updateComponentTranslations('pages/StorageManagement.jsx', [
  { search: 'Storage Management', replace: '{t("storage.title", "Storage Management")}' },
  { search: 'Upgrade Storage', replace: '{t("storage.upgrade", "Upgrade Storage")}' },
  { search: 'Free Space', replace: '{t("storage.free_space", "Free Space")}' }
]);

console.log("Component translation auditing completed!");
