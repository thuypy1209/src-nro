import zipfile
import os

server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
jar_path = os.path.join(server_dir, 'Server.jar')
target_classes = os.path.join(server_dir, 'target', 'classes')
temp_jar = os.path.join(server_dir, 'Server_rebuilt.jar')

# Collect all files from target_classes first
classes_map = {}
for root, dirs, files in os.walk(target_classes):
    for f in files:
        full_p = os.path.join(root, f)
        rel_p = os.path.relpath(full_p, target_classes).replace('\\', '/')
        classes_map[rel_p] = full_p

# Read original jar and replace any files present in classes_map, keep others (libs, resources)
with zipfile.ZipFile(jar_path, 'r') as zin, zipfile.ZipFile(temp_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    seen = set()
    for item in zin.infolist():
        name = item.filename
        if name in seen:
            continue
        seen.add(name)
        if name in classes_map:
            # Write new class file
            zout.write(classes_map[name], name)
        else:
            # Copy from original jar
            data = zin.read(name)
            zout.writestr(item, data)
    
    # Write any classes in classes_map not already seen
    for name, full_p in classes_map.items():
        if name not in seen:
            zout.write(full_p, name)
            seen.add(name)

# Replace Server.jar with Server_rebuilt.jar
os.replace(temp_jar, jar_path)
print("Rebuilt Server.jar cleanly with zero duplicates!")
