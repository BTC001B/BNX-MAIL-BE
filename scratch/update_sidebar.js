const fs = require('fs');
const sidebarPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/SideBar.jsx';
let content = fs.readFileSync(sidebarPath, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useAuth } from "../context/AuthContext";',
    'import { useAuth } from "../context/AuthContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const SideBar = ({ isDesktopOpen, isMobileOpen, onCloseMobile, onOpenNotes }) => {',
    'const SideBar = ({ isDesktopOpen, isMobileOpen, onCloseMobile, onOpenNotes }) => {\n  const { t } = useTranslation();'
  );
}

// Add helper to translate sidebar item names
const translateItemCode = `  const getItemLabel = (name) => {
    switch (name) {
      case 'All Inbox': return t('sidebar.all_inbox', 'All Inbox');
      case 'Inbox': return t('sidebar.inbox', 'Inbox');
      case 'Analytics': return t('sidebar.analytics', 'Analytics');
      case 'Starred': return t('sidebar.starred', 'Starred');
      case 'Sent': return t('sidebar.sent', 'Sent');
      case 'Drafts': return t('sidebar.drafts', 'Drafts');
      case 'Snoozed': return t('sidebar.snoozed', 'Snoozed');
      case 'Scheduled': return t('sidebar.scheduled', 'Scheduled');
      case 'Archive': return t('sidebar.archive', 'Archive');
      case 'Spam': return t('sidebar.spam', 'Spam');
      case 'Trash': return t('sidebar.trash', 'Trash');
      case 'Unread': return t('sidebar.unread', 'Unread');
      case 'Mail Backup': return t('sidebar.mail_backup', 'Mail Backup');
      case 'Groups': return t('sidebar.groups', 'Groups');
      case 'Chat Room': return t('sidebar.chat_room', 'Chat Room');
      case 'Casbox': return t('sidebar.casbox', 'Casbox');
      case 'Vault': return t('sidebar.vault', 'Vault');
      case 'Storage Management': return t('sidebar.storage_management', 'Storage Management');
      case 'Subscriptions': return t('sidebar.subscriptions', 'Subscriptions');
      case 'Settings': return t('sidebar.settings', 'Settings');
      case 'Support & Help': return t('sidebar.support', 'Support & Help');
      default: return name;
    }
  };`;

if (!content.includes('getItemLabel')) {
  content = content.replace(
    'const labelMenuRef = useRef(null);',
    'const labelMenuRef = useRef(null);\n\n' + translateItemCode
  );
}

// Replace sidebar item name rendering with getItemLabel(item.name)
content = content.replaceAll('{item.name}', '{getItemLabel(item.name)}');
content = content.replace('>Compose</span>', '>{t("common.compose", "Compose")}</span>');

fs.writeFileSync(sidebarPath, content, 'utf8');
console.log("SideBar.jsx updated successfully!");
