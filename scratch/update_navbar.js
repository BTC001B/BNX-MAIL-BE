const fs = require('fs');
const navPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/NavBar.jsx';
let content = fs.readFileSync(navPath, 'utf8');

if (!content.includes('useTranslation')) {
  content = content.replace(
    'import { useAuth } from "../context/AuthContext";',
    'import { useAuth } from "../context/AuthContext";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

if (!content.includes('const { t } = useTranslation()')) {
  content = content.replace(
    'const NavBar = ({ searchQuery, setSearchQuery, onOpenMenu, onToggleDesktopSidebar, onToggleBitToolSidebar, onOpenNotes }) => {',
    'const NavBar = ({ searchQuery, setSearchQuery, onOpenMenu, onToggleDesktopSidebar, onToggleBitToolSidebar, onOpenNotes }) => {\n  const { t } = useTranslation();'
  );
}

// Replace search input placeholder
content = content.replace(
  'placeholder="Search mail..."',
  'placeholder={t("navbar.search_placeholder", "Search mail...")}'
);

content = content.replace(
  'placeholder="Search BNX Mail..."',
  'placeholder={t("navbar.search_placeholder", "Search mail...")}'
);

content = content.replace(
  '>Logout</button>',
  '>{t("navbar.logout", "Logout")}</button>'
);

fs.writeFileSync(navPath, content, 'utf8');
console.log("NavBar.jsx updated successfully!");
