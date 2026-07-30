import codecs
import re

files = [
    "app/src/main/java/com/app/ui/viewmodels/TransactionViewModel.kt",
    "app/src/main/java/com/app/ui/viewmodels/WalletViewModel.kt",
    "app/src/main/java/com/app/ui/viewmodels/SettingsViewModel.kt",
    "app/src/main/java/com/app/ui/viewmodels/SyncViewModel.kt",
    "app/src/main/java/com/app/ui/viewmodels/AiAdvisorViewModel.kt"
]

for file_path in files:
    with codecs.open(file_path, "r", "utf-8") as f:
        content = f.read()

    # Replace GoogleSignIn calls
    pattern = r"val account = \s*com\.google\.android\.gms\.auth\.api\.signin\.GoogleSignIn\.getLastSignedInAccount\(.*?\)\?\.account"
    replacement = "val email = repository.getSetting(\"google_account_email\")\n                val account = if (email != null) android.accounts.Account(email, \"com.google\") else null"
    
    content = re.sub(pattern, replacement, content)

    with codecs.open(file_path, "w", "utf-8") as f:
        f.write(content)

print("Updated viewmodels")
