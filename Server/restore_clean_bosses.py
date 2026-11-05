import shutil
import os

nro1_boss = r'C:\Users\ADMIN\Desktop\NRO1\SRC NRO NEW 01\Server\src\main\java\nro\models\boss'
new_boss = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\models\boss'

boss_items = [
    'BossData.java',
    'broly',
    'cell',
    'cold',
    'nappa'
]

for item in boss_items:
    src_p = os.path.join(nro1_boss, item)
    dst_p = os.path.join(new_boss, item)
    if os.path.isdir(src_p):
        if os.path.exists(dst_p):
            shutil.rmtree(dst_p)
        shutil.copytree(src_p, dst_p)
        print(f"Restored directory: {item}")
    else:
        shutil.copy2(src_p, dst_p)
        print(f"Restored file: {item}")

print("Done restoring clean boss files!")
