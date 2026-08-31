const fs = require('fs');
const path = require('path');

const feSrc = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src';

// Helper to inject useTranslation import and hook into a file
function updateJsxFile(filePath, transforms) {
  if (!fs.existsSync(filePath)) return;
  let content = fs.readFileSync(filePath, 'utf8');

  // Inject import if not present
  if (!content.includes('useTranslation')) {
    if (content.includes('import React')) {
      content = content.replace(/import React.*?;/, (match) => `${match}\nimport { useTranslation } from "../context/LanguageContext";`);
    } else {
      content = `import { useTranslation } from "../context/LanguageContext";\n${content}`;
    }
  }

  // Perform custom replacements
  for (const transform of transforms) {
    if (typeof transform === 'function') {
      content = transform(content);
    } else if (transform.search && transform.replace) {
      content = content.replace(transform.search, transform.replace);
    }
  }

  fs.writeFileSync(filePath, content, 'utf8');
  console.log(`Updated: ${path.basename(filePath)}`);
}

// 1. BulkActionsToolbar.jsx
updateJsxFile(path.join(feSrc, 'components/BulkActionsToolbar.jsx'), [
  (c) => c.replace(/const BulkActionsToolbar = \(\{/g, 'const BulkActionsToolbar = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {'),
  (c) => c.replace('>Select All</span>', '>{t("bulk_actions.select_all", "Select All")}</span>'),
  (c) => c.replace('>Mark Read</span>', '>{t("bulk_actions.mark_read", "Mark Read")}</span>'),
  (c) => c.replace('>Mark Unread</span>', '>{t("bulk_actions.mark_unread", "Mark Unread")}</span>'),
  (c) => c.replace('>Star</span>', '>{t("bulk_actions.star", "Star")}</span>'),
  (c) => c.replace('>Unstar</span>', '>{t("bulk_actions.unstar", "Unstar")}</span>'),
  (c) => c.replace('>Delete</span>', '>{t("bulk_actions.delete_selected", "Delete")}</span>'),
  (c) => c.replace('>Spam</span>', '>{t("bulk_actions.report_spam", "Spam")}</span>')
]);

// 2. EmailList.jsx
updateJsxFile(path.join(feSrc, 'components/EmailList.jsx'), [
  (c) => c.replace(/const EmailList = \(\{/g, 'const EmailList = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {'),
  (c) => c.replace('>No emails found</div>', '>{t("inbox.no_emails_found", "No emails found")}</div>'),
  (c) => c.replace('>Loading emails...</div>', '>{t("inbox.loading_emails", "Loading emails...")}</div>')
]);

// 3. StorageWidget.jsx
updateJsxFile(path.join(feSrc, 'components/StorageWidget.jsx'), [
  (c) => c.replace(/const StorageWidget = \(\) => \{/g, 'const StorageWidget = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/used of/g, '{t("storage.used_of", "used of")}'),
  (c) => c.replace(/Storage Management/g, '{t("sidebar.storage_management", "Storage Management")}')
]);

// 4. StorageCard.jsx
updateJsxFile(path.join(feSrc, 'components/StorageCard.jsx'), [
  (c) => c.replace(/const StorageCard = \(\{/g, 'const StorageCard = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {'),
  (c) => c.replace(/Upgrade Storage/g, '{t("storage.upgrade", "Upgrade Storage")}'),
  (c) => c.replace(/Storage Limit/g, '{t("storage.storage_limit", "Storage Limit")}')
]);

// 5. AppLauncher.jsx
updateJsxFile(path.join(feSrc, 'components/AppLauncher.jsx'), [
  (c) => c.replace(/const AppLauncher = \(\{/g, 'const AppLauncher = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {'),
  (c) => c.replace(/Product Launcher/g, '{t("navbar.product_launcher", "Product Launcher")}'),
  (c) => c.replace(/Apps & Services/g, '{t("navbar.apps", "Apps & Services")}')
]);

// 6. ContactPanel.jsx
updateJsxFile(path.join(feSrc, 'components/ContactPanel.jsx'), [
  (c) => c.replace(/const ContactPanel = \(\{/g, 'const ContactPanel = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {'),
  (c) => c.replace(/placeholder="Search contacts\.\.\."/g, 'placeholder={t("contacts.search_contacts", "Search contacts...")}'),
  (c) => c.replace(/>Contacts<\/h3>/g, '>{t("contacts.title", "Contacts")}</h3>'),
  (c) => c.replace(/>Add Contact</g, '>{t("contacts.add_contact", "Add Contact")}<')
]);

// 7. Pages: Starred, Draft, Send, Trash, Spam, Snoozed, Scheduled, Archive, AllMail, Unread
const mailPages = ['Starred', 'Draft', 'Send', 'Trash', 'Spam', 'Snoozed', 'Scheduled', 'Archive', 'AllMail', 'Unread'];
mailPages.forEach(pageName => {
  const fileP = path.join(feSrc, `pages/${pageName}.jsx`);
  if (!fs.existsSync(fileP)) return;
  updateJsxFile(fileP, [
    (c) => {
      const reg = new RegExp(`const ${pageName} = \\(\\{`, 'g');
      return c.replace(reg, `const ${pageName} = ({\n  ...props\n}) => {\n  const { t } = useTranslation();\n  const {`);
    },
    (c) => {
      const reg2 = new RegExp(`const ${pageName} = \\(\\) => \\{`, 'g');
      return c.replace(reg2, `const ${pageName} = () => {\n  const { t } = useTranslation();`);
    }
  ]);
});

// 8. Templates.jsx
updateJsxFile(path.join(feSrc, 'pages/Templates.jsx'), [
  (c) => c.replace(/const Templates = \(\) => \{/g, 'const Templates = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Templates<\/h2>/g, '>{t("templates.title", "Templates")}</h2>'),
  (c) => c.replace(/>Create Template</g, '>{t("templates.create_template", "Create Template")}<')
]);

// 9. Casbox.jsx
updateJsxFile(path.join(feSrc, 'pages/Casbox.jsx'), [
  (c) => c.replace(/const Casbox = \(\) => \{/g, 'const Casbox = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Casbox<\/h2>/g, '>{t("casbox.title", "Casbox")}</h2>')
]);

// 10. Vault.jsx
updateJsxFile(path.join(feSrc, 'pages/Vault.jsx'), [
  (c) => c.replace(/const Vault = \(\) => \{/g, 'const Vault = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Vault<\/h2>/g, '>{t("sidebar.vault", "Vault")}</h2>')
]);

// 11. Subscriptions.jsx
updateJsxFile(path.join(feSrc, 'pages/Subscriptions.jsx'), [
  (c) => c.replace(/const Subscriptions = \(\) => \{/g, 'const Subscriptions = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Subscriptions<\/h2>/g, '>{t("sidebar.subscriptions", "Subscriptions")}</h2>')
]);

// 12. Notification.jsx
updateJsxFile(path.join(feSrc, 'pages/Notification.jsx'), [
  (c) => c.replace(/const Notification = \(\) => \{/g, 'const Notification = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Notifications<\/h2>/g, '>{t("notifications.title", "Notifications")}</h2>')
]);

// 13. Support.jsx
updateJsxFile(path.join(feSrc, 'pages/Support.jsx'), [
  (c) => c.replace(/const Support = \(\) => \{/g, 'const Support = () => {\n  const { t } = useTranslation();'),
  (c) => c.replace(/>Support & Help<\/h2>/g, '>{t("sidebar.support", "Support & Help")}</h2>')
]);

console.log("All components and pages updated with useTranslation!");
