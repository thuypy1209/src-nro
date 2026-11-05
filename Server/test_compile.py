import os
import subprocess
import glob

server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
src_dir = os.path.join(server_dir, 'src', 'main', 'java')
jar_path = os.path.join(server_dir, 'Server.jar')

# Collect all java files
java_files = []
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

print(f"Total Java files to compile: {len(java_files)}")

# Write file list to sources.txt
sources_file = os.path.join(server_dir, 'sources_to_compile.txt')
with open(sources_file, 'w', encoding='utf-8') as f:
    for jf in java_files:
        rel = os.path.relpath(jf, server_dir).replace('\\', '/')
        f.write(f'"{rel}"\n')

# Compile
build_temp = os.path.join(server_dir, 'target', 'classes')
os.makedirs(build_temp, exist_ok=True)

cp = f"{jar_path};{os.path.join(server_dir, 'lib', '*')}"

cmd = ['javac', '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '-cp', cp, '-d', build_temp, f'@{sources_file}']
print("Running javac...")
res = subprocess.run(cmd, cwd=server_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
if res.returncode == 0:
    print("COMPILE SUCCESS! Output classes in target/classes")
else:
    print(f"COMPILE FAILED (code {res.returncode}):")
    errors = res.stderr.splitlines()
    for e in errors:
        print(e)
