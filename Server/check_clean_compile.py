import subprocess, os
server_dir = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server'
jar_path = os.path.join(server_dir, 'Server.jar')
sources_file = os.path.join(server_dir, 'sources_to_compile.txt')
build_temp = os.path.join(server_dir, 'target', 'classes')
lib_cp = os.path.join(server_dir, 'lib', '*')
cp = f"{jar_path};{lib_cp}"

# Regenerate sources_to_compile.txt
src_dir = os.path.join(server_dir, 'src', 'main', 'java')
java_files = []
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

with open(sources_file, 'w', encoding='utf-8') as f:
    for jf in java_files:
        rel = os.path.relpath(jf, server_dir).replace('\\', '/')
        f.write(f'"{rel}"\n')

cmd = ['javac', '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '-cp', cp, '-d', build_temp, f'@{sources_file}']
res = subprocess.run(cmd, cwd=server_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
print("Return code:", res.returncode)
print("STDERR:\n", res.stderr)
