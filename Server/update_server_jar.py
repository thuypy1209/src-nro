import zipfile
import os

server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
target_classes = os.path.join(server_dir, 'target', 'classes')
jar_path = os.path.join(server_dir, 'Server.jar')

updated_count = 0
with zipfile.ZipFile(jar_path, 'a') as z:
    for root, dirs, files in os.walk(target_classes):
        for f in files:
            full_p = os.path.join(root, f)
            rel_p = os.path.relpath(full_p, target_classes).replace('\\', '/')
            z.write(full_p, rel_p)
            updated_count += 1

print(f"Updated {updated_count} classes into Server.jar successfully!")
