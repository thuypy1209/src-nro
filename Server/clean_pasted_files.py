import os
import shutil
import datetime

server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
src_dir = os.path.join(server_dir, 'src', 'main', 'java')
quarantine_dir = os.path.join(server_dir, 'pasted_files_from_old_src')
os.makedirs(quarantine_dir, exist_ok=True)

target_time = datetime.datetime(2026, 11, 2, 14, 45).timestamp()

moved_files = 0
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith('.java'):
            p = os.path.join(root, f)
            ctime = os.path.getctime(p)
            if ctime >= target_time:
                # Keep XENCON1..6 in cell as they were just renamed
                if 'cell' in root and f.startswith('XENCON'):
                    continue
                rel = os.path.relpath(p, src_dir)
                dest = os.path.join(quarantine_dir, rel)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                shutil.move(p, dest)
                moved_files += 1

print(f"Successfully moved {moved_files} pasted files to {quarantine_dir}")

# Remove empty directories in src_dir
removed_dirs = 0
for root, dirs, files in os.walk(src_dir, topdown=False):
    for d in dirs:
        dp = os.path.join(root, d)
        if not os.listdir(dp):
            os.rmdir(dp)
            removed_dirs += 1

print(f"Cleaned up {removed_dirs} empty directories")
