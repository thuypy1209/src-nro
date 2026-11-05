import os
import shutil

models_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\models'
quarantine_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\quarantine_pasted_models'
os.makedirs(quarantine_dir, exist_ok=True)

# Original packages that belong to nro.models
original_models = {
    'boss', 'clan', 'dragon', 'intrinsic', 'item', 'kigui',
    'map', 'mob', 'npc', 'npc_list', 'player', 'pvp', 'shop',
    'skill', 'task', 'SieuHang'
}

original_files = {
    'Part.java', 'PartDetail.java', 'PartImage.java', 'PartManager.java', 'Template.java'
}

# Scan everything currently in models_dir
for item in os.listdir(models_dir):
    full_path = os.path.join(models_dir, item)
    if os.path.isdir(full_path):
        if item not in original_models:
            dest = os.path.join(quarantine_dir, item)
            print(f"Quarantining folder: {item} -> {dest}")
            shutil.move(full_path, dest)
    else:
        if item not in original_files and item.endswith('.java'):
            dest = os.path.join(quarantine_dir, item)
            print(f"Quarantining file: {item} -> {dest}")
            shutil.move(full_path, dest)

print("\nDone quarantine! Current models folder contains:")
for item in sorted(os.listdir(models_dir)):
    print(" -", item)
