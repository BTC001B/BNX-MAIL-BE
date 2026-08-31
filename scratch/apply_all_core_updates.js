const { execSync } = require('child_process');

console.log("Applying core updates...");
execSync('node scratch/update_app.js', { stdio: 'inherit' });
execSync('node scratch/update_constants_api.js', { stdio: 'inherit' });
execSync('node scratch/update_settings_page.js', { stdio: 'inherit' });
execSync('node scratch/update_sidebar.js', { stdio: 'inherit' });
execSync('node scratch/update_navbar.js', { stdio: 'inherit' });
execSync('node scratch/update_mail_backup.js', { stdio: 'inherit' });
execSync('node scratch/update_storage_management.js', { stdio: 'inherit' });
execSync('node scratch/update_email_details.js', { stdio: 'inherit' });
execSync('node scratch/update_floating_compose.js', { stdio: 'inherit' });
execSync('node scratch/update_inbox.js', { stdio: 'inherit' });
console.log("All core updates applied!");
