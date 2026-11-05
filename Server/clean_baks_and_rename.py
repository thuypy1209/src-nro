import os

cell_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\models\boss\cell'
src_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java'

# Remove all .bak files
bak_count = 0
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if '.bak' in f:
            os.remove(os.path.join(root, f))
            bak_count += 1
print(f"Removed {bak_count} .bak files")

# Rename XenCon1..6 to XENCON1..6
for i in range(1, 7):
    old_f = os.path.join(cell_dir, f"XenCon{i}.java")
    tmp_f = os.path.join(cell_dir, f"tmp_XENCON_{i}.java")
    new_f = os.path.join(cell_dir, f"XENCON{i}.java")
    if os.path.exists(old_f):
        os.rename(old_f, tmp_f)
        os.rename(tmp_f, new_f)
        print(f"Renamed XenCon{i}.java -> XENCON{i}.java")
