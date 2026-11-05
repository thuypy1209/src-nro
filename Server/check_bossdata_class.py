import zipfile
import re

jar_path = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\Server.jar'
with zipfile.ZipFile(jar_path, 'r') as z:
    data = z.read('nro/models/boss/BossData.class')

strings = re.findall(b'[\x20-\x7e]{3,}', data)
print("Strings in BossData.class from Server.jar:")
for s in strings:
    s_str = s.decode('latin1')
    if '/' in s_str or 'Type' in s_str or 'boss' in s_str:
        print(" ", s_str)
